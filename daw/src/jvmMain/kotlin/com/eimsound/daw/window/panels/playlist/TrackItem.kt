@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.eimsound.daw.window.panels.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import com.eimsound.daw.actions.doAddOrRemoveTrackAction
import com.eimsound.daw.actions.doReorderAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.components.LocalFloatingLayerProvider
import com.eimsound.daw.components.IconButton
import com.eimsound.daw.components.VolumeSlider
import com.eimsound.daw.components.icons.Crown
import com.eimsound.daw.components.menus.openTrackMenu
import com.eimsound.daw.components.utils.*
import com.eimsound.daw.utils.isCrossPlatformAltPressed
import com.eimsound.daw.window.dialogs.openColorPicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

internal class TrackMoveFlags(val parent: Track) {
    var isAbove by mutableStateOf(false)
    var isChild by mutableStateOf(false)
}
internal data class TrackToHeight(val track: Track, val height: Float)

private var pointerIcon by mutableStateOf<PointerIcon?>(null)

private const val MIN_TRACK_HEIGHT = 30
private suspend fun AwaitPointerEventScope.handleDrag(playlist: Playlist, track: Track, parentTrack: Track,
                                                      isDragging: MutableState<Boolean>) {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Final)
        val primaryButtonCausesDown = event.changes.fastAll { it.type == PointerType.Mouse }
        val changedToDown = event.changes.fastAll { it.changedToDownIgnoreConsumed() }
        val curHeight = if (track.height == 0) playlist.trackHeight.value else track.height.toFloat()
        if (event.changes.first().position.y / density > curHeight - 4) {
            if (pointerIcon == null) pointerIcon = PointerIcon.VerticalResize
        } else if (pointerIcon != null) pointerIcon = null
    } while (!(changedToDown && (event.buttons.isPrimaryPressed || !primaryButtonCausesDown)))
    val down = event.changes[0]
    awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false) { change, _ ->
        playlist.apply {
            val curHeight = if (track.height == 0) trackHeight.value else track.height.toFloat()
            if (change.position.y / density > curHeight - 4) {
                drag(down.id) {
                    track.height = (curHeight + (it.position.y - change.position.y) / density)
                        .roundToInt().coerceAtLeast(MIN_TRACK_HEIGHT)
                }
                return
            }
            getAllTrackHeights(density)
            isDragging.value = true
            var lastTrack: Track? = null
            var lastFlags: TrackMoveFlags? = null
            var currentY: Float
            drag(down.id) {
                currentY = dragStartY + it.position.y - change.position.y
                // binary search drop track by trackHeights and currentY
                val cur = binarySearchTrackByHeight(currentY)
                val dropTrack = trackHeights[cur].track
                lastFlags?.isAbove = false
                lastFlags?.isChild = false
                if (dropTrack == track) return@drag
                lastTrack = dropTrack
                val flags0 = trackMovingFlags[dropTrack] ?: return@drag
                lastFlags = flags0
                if (currentY - (if (cur == 0) 0F else trackHeights[cur - 1].height) < 6) flags0.isAbove = true
                else flags0.isChild = true
            }
            if (lastTrack != null) {
                // dfs search drop track is not the child of current track
                var dropTrack = lastTrack
                var flag = false
                while (dropTrack != null) {
                    if (dropTrack == track) {
                        flag = true
                        break
                    }
                    dropTrack = trackMovingFlags[dropTrack]?.parent
                }
                if (!flag) lastFlags?.let {
                    if (it.isAbove) track.doReorderAction(
                        parentTrack.subTracks, it.parent.subTracks,
                        it.parent.subTracks.indexOf(lastTrack)
                    )
                    else if (it.isChild) track.doReorderAction(parentTrack.subTracks, lastTrack!!.subTracks)
                }
            }
            lastFlags?.isAbove = false
            lastFlags?.isChild = false
        }
        isDragging.value = false
    }
}

@Composable
private fun TrackLevel(track: Track) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxHeight().width(5.dp).graphicsLayer {  }) {
        val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
        drawRect(primaryColor, Offset(0F, y), Size(size.width, size.height - y))
        // drawLine(outlineColor, Offset(size.width / 2, y), Offset(size.width / 2, size.height), 0.5F)
        // drawLine(outlineColor, Offset.Zero, Offset(0F, size.height), 0.5F)
    }
}

private val TRACK_ITEM_INDEX_WIDTH = 20.dp

@Composable
private fun SubTracks(track: Track, playlist: Playlist, depth: Int) {
    if (!track.collapsed && track.subTracks.isNotEmpty()) track.subTracks.forEachIndexed { i, it -> key(it.id) {
        Divider(Modifier.offset(TRACK_ITEM_INDEX_WIDTH * (depth + 1)))
        TrackItem(playlist, it, track, i, depth + 1)
    } }
}

@Composable
private fun TrackIndex(track: Track, parentTrack: Track, index: Int) {
    val color = track.color
    val localFloatingLayerProvider = LocalFloatingLayerProvider.current

    Box(Modifier.fillMaxHeight().width(TRACK_ITEM_INDEX_WIDTH).background(color)
        .onRightClickOrLongPress { it, m ->
            localFloatingLayerProvider.openTrackMenu(it, track, parentTrack.subTracks, index, m.isCrossPlatformAltPressed)
        }
        .clickableWithIcon {
            localFloatingLayerProvider.openColorPicker(color) { track.color = it }
        }
    ) {
        Text((index + 1).toString(),
            Modifier.align(Alignment.Center),
            color.toOnSurfaceColor(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun TrackItemControllers(track: Track) {
    Row(Modifier.fillMaxWidth().offset((-4).dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ track.isSolo = !track.isSolo }, 24.dp) {
            Icon(
                Crown,
                null, Modifier.size(18.dp),
                if (track.isSolo) MaterialTheme.colorScheme.warning else MaterialTheme.colorScheme.primary.copy(0.5F)
            )
        }
        IconButton({ track.isDisabled = !track.isDisabled }, 24.dp) {
            Icon(
                if (track.isDisabled) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                null, Modifier.size(18.dp),
                if (track.isDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        VolumeSlider(track, Modifier.weight(1F), false, !track.isDisabled)
    }
}

@Composable
private fun RowScope.TrackCollapsedButton(track: Track) {
    IconButton({
        track.collapsed = !track.collapsed
    }, 28.dp, Modifier.align(Alignment.CenterVertically).offset((-4).dp), track.subTracks.isNotEmpty()) {
        val deg by animateFloatAsState(if (track.collapsed) 0F else 180F)
        if (track.subTracks.isNotEmpty()) Icon(
            Icons.Filled.ExpandLess, null,
            Modifier.rotate(deg),
            MaterialTheme.colorScheme.primary.copy(0.4F)
        )
    }
}

@Composable
private fun TrackName(track: Track) {
    BasicTextField(track.name, { track.name = it },
        Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge.copy(
            if (track.isDisabled) LocalContentColor.current.copy(alpha = 0.5F) else LocalContentColor.current,
            textDecoration = if (track.isDisabled) TextDecoration.LineThrough else null
        )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TrackItem(playlist: Playlist, track: Track, parentTrack: Track, index: Int, depth: Int = 0) {
    val isDragging = remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val flags = playlist.trackMovingFlags[track]
    val alpha by animateFloatAsState(flags?.let { if (it.isAbove || it.isChild) 0.4F else 0F } ?: 0F)
    val color = track.color

    playlist.apply {
        val curHeight = if (track.height == 0) trackHeight else track.height.dp
        Row(Modifier.height(curHeight)
            .padding(start = TRACK_ITEM_INDEX_WIDTH * depth)
            .background(color.copy(animateFloatAsState(if (EchoInMirror.selectedTrack == track) 0.1F else 0F).value))
            .onPointerEvent(PointerEventType.Press) { EchoInMirror.selectedTrack = track }
            .drawWithContent {
                drawContent()
                if (flags == null || !(flags.isChild || flags.isAbove)) return@drawWithContent
                val left = TRACK_ITEM_INDEX_WIDTH.toPx()
                if (flags.isChild) drawRect(
                    primaryColor,
                    Offset(left, 0F),
                    Size(size.width - left, size.height),
                    alpha = alpha
                )
                else if (flags.isAbove) drawRect(
                    primaryColor,
                    Offset(left, 0F),
                    Size(size.width - left, 2F),
                    alpha = alpha
                )
            }
            .pointerInput(track, parentTrack) {
                trackMovingFlags[track] = TrackMoveFlags(parentTrack)
                awaitEachGesture { handleDrag(playlist, track, parentTrack, isDragging) }
            }
            .alpha(animateFloatAsState(if (isDragging.value) 0.3F else 1F).value)
        ) {
            TrackIndex(track, parentTrack, index)
            Column(
                Modifier.weight(1F).fillMaxHeight().padding(8.dp, 5.dp, 2.dp, 5.dp),
                if (curHeight.value >= 50) Arrangement.SpaceBetween else Arrangement.Center
            ) {
                TrackName(track)
                if (curHeight.value >= 50) TrackItemControllers(track)
            }

            TrackCollapsedButton(track)
            TrackLevel(track)
        }

        SubTracks(track, playlist, depth)
    }
}

@Composable
private fun TrackItemsContent(playlist: Playlist) {
    Divider()
    val bus = EchoInMirror.bus!!
    bus.subTracks.forEachIndexed { i, it ->
        key(it.id) {
            TrackItem(playlist, it, bus, i)
            Divider()
        }
    }
    TextButton({
        runBlocking {
            EchoInMirror.bus!!.subTracks.doAddOrRemoveTrackAction(TrackManager.instance.createTrack())
        }
    }, Modifier.fillMaxWidth()) {
        Text("创建轨道")
    }
}
@Composable
internal fun TrackItems(playlist: Playlist) {
    val scope = rememberCoroutineScope()
    playlist.apply {
        Column(Modifier.pointerInput(scope) {
            detectDragGestures({ dragStartY = it.y + verticalScrollState.value }) { change, _ ->
                if (change.position.y < 10) scope.launch { verticalScrollState.scrollBy(-3F) }
                else if (change.position.y > size.height - 10) scope.launch { verticalScrollState.scrollBy(3F) }
            }
        }.verticalScroll(verticalScrollState).let {
            if (pointerIcon == null) it else it.pointerHoverIcon(pointerIcon!!)
        }) {
            TrackItemsContent(playlist)
        }
    }
}
