package com.eimsound.daw.window.panels.playlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.actions.doClipsAmountAction
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.FileExtensionManager
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.EditorExtension
import com.eimsound.daw.api.window.EditorExtensions
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.GlobalDropTarget
import com.eimsound.daw.components.dragdrop.LocalGlobalDragAndDrop
import com.eimsound.daw.components.splitpane.HorizontalSplitPane
import com.eimsound.daw.components.splitpane.SplitPaneState
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.toOnSurfaceColor
import com.eimsound.daw.dawutils.editorToolHoverIcon
import com.eimsound.daw.dawutils.openMaxValue
import com.eimsound.daw.utils.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.*

val playlistTrackControllerMinWidth = 240.dp

class Playlist : Panel, MultiSelectableEditor {
    override val name = "Playlist"
    override val direction = PanelDirection.Horizontal
    val selectedClips = mutableStateSetOf<TrackClip<*>>()
    var noteWidth = mutableStateOf(0.2.dp)
    var trackHeight by mutableStateOf(50.dp)
    val verticalScrollState = ScrollState(0)
    val horizontalScrollState = ScrollState(0).apply {
        openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
    }

    internal var action by mutableStateOf(EditAction.NONE)
    internal var contentWidth by mutableStateOf(0.dp)
    internal var selectionX by mutableStateOf(0F)
    internal var selectionY by mutableStateOf(0F)
    internal var selectionStartX = 0F
    internal var selectionStartY = 0F
    internal var deltaX by mutableStateOf(0)
    internal var deltaY by mutableStateOf(0)
    internal val trackHeights = arrayListOf<TrackToHeight>()
    internal val deletionList = mutableStateSetOf<TrackClip<*>>()
    internal val disableList = mutableStateSetOf<TrackClip<*>>()
    internal val trackMovingFlags = WeakHashMap<Track, TrackMoveFlags>()

    companion object {
        var copiedClips: List<TrackClip<*>>? = null
        val playListExtensions: MutableList<EditorExtension> = mutableStateListOf()
        val playlistTrackControllerPanelState = SplitPaneState()
    }

    @Composable
    private fun PlaylistPlayHead() {
        PlayHead(noteWidth, horizontalScrollState,
            (EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.ppq).toFloat(),
            contentWidth)
    }

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.QueueMusic, "Playlist")
    }

    // Focusable
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        val focusManager = LocalFocusManager.current
        HorizontalSplitPane(Modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
            EchoInMirror.windowManager.activePanel = this@Playlist
            focusManager.clearFocus(true)
        }, playlistTrackControllerPanelState) {
            first(playlistTrackControllerMinWidth) {
                Surface(
                    Modifier.fillMaxHeight().zIndex(5f),
                    shadowElevation = 2.dp, tonalElevation = 2.dp
                ) {
                    Column {
                        Surface(shadowElevation = 2.dp, tonalElevation = 4.dp) {
                            Row(Modifier.height(TIMELINE_HEIGHT).fillMaxWidth().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                NoteWidthSlider(noteWidth)
                            }
                        }
                        TrackItems(this@Playlist)
                    }
                }
            }

            second {
                PlaylistContents()
            }
        }
    }

    override fun copy() {
        if (selectedClips.isEmpty()) return
        val startTime = selectedClips.minOf { it.time }
        copiedClips = selectedClips.map { it.copy(it.time - startTime) }
    }

    override fun paste() {
        if (copiedClips?.isEmpty() == true) return
        val startTime = EchoInMirror.currentPosition.timeInPPQ.fitInUnitCeil(EchoInMirror.editUnit)
        val clips = copiedClips!!.map { it.copy(time = it.time + startTime) }
        clips.doClipsAmountAction(false)
        selectedClips.clear()
        selectedClips.addAll(clips)
    }

    override fun delete() {
        if (selectedClips.isEmpty()) return
        selectedClips.doClipsAmountAction(true)
        selectedClips.clear()
    }

    override fun selectAll() { EchoInMirror.bus!!.subTracks.forEach(::selectAllClipsInTrack) }

    private fun selectAllClipsInTrack(track: Track) {
        selectedClips.addAll(track.clips)
        track.subTracks.forEach(::selectAllClipsInTrack)
    }

    @Composable
    private fun PlaylistContents() {
        Box(Modifier.fillMaxSize().clipToBounds().scalableNoteWidth(noteWidth, horizontalScrollState)) {
            Column {
                val localDensity = LocalDensity.current
                Timeline(Modifier.zIndex(3f), noteWidth, horizontalScrollState, EchoInMirror.currentPosition.projectRange,
                    editUnit = EchoInMirror.editUnit, barPPQ = EchoInMirror.currentPosition.oneBarPPQ,
                    onTimeChange = EchoInMirror.currentPosition::setCurrentTime
                ) {
                    EchoInMirror.currentPosition.projectRange = it
                }
                val coroutineScope = rememberCoroutineScope()
                Box(Modifier.weight(1f).pointerInput(coroutineScope) {
                    handleMouseEvent(this@Playlist, coroutineScope)
                }.onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }) {
                    EchoInMirror.currentPosition.apply {
                        EditorGrid(noteWidth, horizontalScrollState, projectRange, ppq, timeSigDenominator, timeSigNumerator)
                    }
                    val width = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ)
                        .coerceAtLeast(contentWidth)
                    remember(width, localDensity) {
                        with (localDensity) { horizontalScrollState.openMaxValue = width.toPx().toInt() }
                    }
                    playListExtensions.EditorExtensions(true)
                    TrackContents()
                    playListExtensions.EditorExtensions(false)
                    DropTarget()
                    TrackSelection(this@Playlist, localDensity, horizontalScrollState, verticalScrollState)
                    PlaylistPlayHead()
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.TopEnd).fillMaxHeight()
                    )
                }
            }
            HorizontalScrollbar(
                rememberScrollbarAdapter(horizontalScrollState),
                Modifier.align(Alignment.TopStart).fillMaxWidth()
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    private fun DropTarget() {
        val density = LocalDensity.current.density
        GlobalDropTarget({ obj, it ->
            var path = obj as? Path
            var data: Any? = null
            if (path == null && obj is Pair<*, *>) {
                path = obj.first as? Path
                data = obj.second
            }
            val path0 = path ?: return@GlobalDropTarget
            val handler = FileExtensionManager.getHandler(path0) ?: return@GlobalDropTarget
            getAllTrackHeights(density)
            val track = trackHeights[binarySearchTrackByHeight(it.y).coerceAtMost(trackHeights.size - 1)].track
            val time = ((it.x + horizontalScrollState.value) / density / noteWidth.value.value).fitInUnit(EchoInMirror.editUnit)
            trackHeights.clear()

            GlobalScope.launch(Dispatchers.IO) {
                val clip = handler.createClip(path0, data)
                listOf(ClipManager.instance.createTrackClip(
                    clip,
                    time,
                    if (clip.maxDuration == -1) clip.duration.coerceAtLeast(EchoInMirror.currentPosition.oneBarPPQ) else clip.duration,
                    0, track
                )).doClipsAmountAction(false)
            }
        }) {
            val globalDragAndDrop = LocalGlobalDragAndDrop.current
            val isDroppable = remember(it == null) {
                if (it == null) {
                    trackHeights.clear()
                    return@remember null
                }
                val obj = globalDragAndDrop.dataTransfer
                var path = obj as? Path
                if (path == null && obj is Pair<*, *>) path = obj.first as? Path
                val result = FileExtensionManager.getHandler(path ?: return@remember null) != null
                if (result) getAllTrackHeights(density)
                result
            }

            Box(Modifier.fillMaxSize().run {
                if (isDroppable == false) background(MaterialTheme.colorScheme.error.copy(0.3F)) else this
            }) {
                if (isDroppable == false) {
                    Icon(
                        Icons.Filled.DoNotTouch, null, Modifier.align(Alignment.Center),
                        MaterialTheme.colorScheme.onError
                    )
                } else if (isDroppable == true && it != null) {
                    val obj = trackHeights[binarySearchTrackByHeight(it.y).coerceAtMost(trackHeights.size - 1)]
                    val height = if (obj.track.height == 0) trackHeight else obj.track.height.dp
                    Box(Modifier
                        .offset(
                            noteWidth.value * (it.x / density / noteWidth.value.value).fitInUnit(EchoInMirror.editUnit),
                            ((obj.height - verticalScrollState.value) / density).dp - height
                        )
                        .size(noteWidth.value * EchoInMirror.currentPosition.oneBarPPQ, height)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(obj.track.color.copy(0.4F))
                    ) {
                        Icon(
                            Icons.Filled.SystemUpdateAlt, null,
                            Modifier.align(Alignment.Center), obj.track.color.toOnSurfaceColor()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TrackContents() {
        Column(Modifier.verticalScroll(verticalScrollState).fillMaxSize().editorToolHoverIcon(EchoInMirror.editorTool)) {
            Divider()
            var i = 0
            EchoInMirror.bus!!.subTracks.fastForEach {
                key(it) { i += TrackContent(this@Playlist, it, i) }
            }
        }
    }

    private fun getTrackHeights(list: ArrayList<TrackToHeight>, track: Track, density: Float) {
        list.add(TrackToHeight(track, density + (list.lastOrNull()?.height ?: 0F) +
                (if (track.height == 0) trackHeight.value * density else track.height * density)))
        track.subTracks.fastForEach { getTrackHeights(list, it, density) }
    }

    internal fun getAllTrackHeights(density: Float) {
        trackHeights.clear()
        EchoInMirror.bus!!.subTracks.fastForEach { getTrackHeights(trackHeights, it, density) }
    }

    // binary search drop track by trackHeights and currentY
    internal fun binarySearchTrackByHeight(y: Float) = trackHeights.binarySearch { it.height <= y }
}

@Deprecated("Will be replaced with a more flexible system")
val mainPlaylist = Playlist()
