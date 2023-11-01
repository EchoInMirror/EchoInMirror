package com.eimsound.daw.window.panels.playlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.actions.doClipsAmountAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.EditorExtension
import com.eimsound.daw.api.window.EditorExtensions
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.GlobalDropTarget
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.dawutils.openMaxValue
import com.eimsound.daw.utils.*
import java.util.*

val playListExtensions: MutableList<EditorExtension> = mutableStateListOf()

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
    internal var trackHeights = emptyList<TrackToHeight>()
    internal val deletionList = mutableStateSetOf<TrackClip<*>>()
    internal val trackMovingFlags = WeakHashMap<Track, TrackMoveFlags>()

    companion object {
        var copiedClips: List<TrackClip<*>>? = null
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
        Row(Modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
            EchoInMirror.windowManager.activePanel = this@Playlist
            focusManager.clearFocus(true)
        }) {
            Surface(Modifier.width(260.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 2.dp) {
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
                        Column(Modifier.verticalScroll(verticalScrollState).fillMaxSize()) {
                            Divider()
                            var i = 0
                            EchoInMirror.bus!!.subTracks.forEach {
                                key(it.id) { i += TrackContent(this@Playlist, it, i, localDensity) }
                            }
                        }
                        playListExtensions.EditorExtensions(false)
                        GlobalDropTarget({ _, _ ->
//                            if (data.extension.lowercase() == "mid") {
                                // TODO: 导入midi
//                                println(data)
//                            }
                        }) {
//                            println(it)
//                            println(LocalGlobalDragAndDrop.current.dataTransfer)
                            Spacer(Modifier.fillMaxSize())
                        }
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
        doClipsAmountAction(clips, false)
        selectedClips.clear()
        selectedClips.addAll(clips)
    }

    override fun delete() {
        if (selectedClips.isEmpty()) return
        doClipsAmountAction(selectedClips, true)
        selectedClips.clear()
    }

    override fun selectAll() { EchoInMirror.bus!!.subTracks.forEach(::selectAllClipsInTrack) }

    private fun selectAllClipsInTrack(track: Track) {
        selectedClips.addAll(track.clips)
        track.subTracks.forEach(::selectAllClipsInTrack)
    }
}

@Deprecated("Will be replaced with a more flexible system")
val mainPlaylist = Playlist()
