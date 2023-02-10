@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "PrivatePropertyName")

package com.eimsound.daw.window.panels.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.actions.doAddOrRemoveTrackAction
import com.eimsound.daw.actions.doReorderAction
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.components.*
import com.eimsound.daw.components.icons.Crown
import com.eimsound.daw.components.icons.DebugStepOver
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.utils.binarySearch
import com.eimsound.daw.window.dialogs.openColorPicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

private val TRACK_ITEM_ICON_SIZE = Modifier.size(16.dp)
private val TRACK_COLOR_WIDTH = 8.dp

private class TrackMoveFlags(val parent: Track) {
    var isAbove by mutableStateOf(false)
    var isChild by mutableStateOf(false)
}
private val trackMovingFlags = WeakHashMap<Track, TrackMoveFlags>()
internal data class TrackToHeight(val track: Track, val height: Float)
private fun getTrackHeights(list: ArrayList<TrackToHeight>, track: Track, defaultHeight: Float, density: Float) {
    list.add(TrackToHeight(track, density + (list.lastOrNull()?.height ?: 0F) +
            (if (track.height == 0) defaultHeight else track.height.toFloat())))
    track.subTracks.fastForEach { getTrackHeights(list, it, defaultHeight, density) }
}
internal fun getAllTrackHeights(defaultHeight: Float, density: Float): ArrayList<TrackToHeight> {
    val trackHeights = ArrayList<TrackToHeight>()
    EchoInMirror.bus!!.subTracks.fastForEach { getTrackHeights(trackHeights, it, defaultHeight, density) }
    return trackHeights
}

// binary search drop track by trackHeights and currentY
internal fun binarySearchTrackByHeight(trackHeights: ArrayList<TrackToHeight>, y: Float) =
    trackHeights.binarySearch { it.height <= y }

private suspend fun AwaitPointerEventScope.handleDrag(playlist: Playlist, track: Track, parentTrack: Track,
                                                      isDragging: MutableState<Boolean>) {
    val down = awaitFirstDownOnPass(PointerEventPass.Final, false)
    awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false) { change, _ ->
        playlist.apply {
            trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
            isDragging.value = true
            var lastTrack: Track? = null
            var lastFlags: TrackMoveFlags? = null
            var currentY: Float
            drag(down.id) {
                currentY = dragStartY + it.position.y - change.position.y
                // binary search drop track by trackHeights and currentY
                val cur = binarySearchTrackByHeight(trackHeights, currentY)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TrackItem(playlist: Playlist, track: Track, parentTrack: Track, index: Int, depth: Int = 0) {
    val isDragging = remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val flags = trackMovingFlags[track]
    val alpha by animateFloatAsState(flags?.let { if (it.isAbove || it.isChild) 0.4F else 0F } ?: 0F)
    playlist.apply {
        Row(Modifier.height(trackHeight).padding(start = TRACK_COLOR_WIDTH * depth)
            .background(track.color.copy(animateFloatAsState(if (EchoInMirror.selectedTrack == track) 0.1F else 0F).value))
            .onPointerEvent(PointerEventType.Press) { EchoInMirror.selectedTrack = track }
            .drawWithContent {
                drawContent()
                if (flags == null || !(flags.isChild || flags.isAbove)) return@drawWithContent
                val left = TRACK_COLOR_WIDTH.toPx()
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
                forEachGesture { awaitPointerEventScope { handleDrag(playlist, track, parentTrack, isDragging) } }
            }
            .alpha(animateFloatAsState(if (isDragging.value) 0.3F else 1F).value)
        ) {
            val localFloatingDialogProvider = LocalFloatingDialogProvider.current
            Spacer(Modifier.fillMaxHeight().width(8.dp).background(track.color).clickableWithIcon {
                openColorPicker(localFloatingDialogProvider, track.color) { track.color = it }
            })
            Row(Modifier.weight(1F).padding(8.dp, 4.dp)) {
                Text(index.toString(),
                    Modifier.width(20.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Column(Modifier.fillMaxHeight(), Arrangement.SpaceBetween) {
                    Text(track.name, style = MaterialTheme.typography.labelLarge, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    if (trackHeight.value > 54) SegmentedButtons {
                        SegmentedButton({ track.isMute = !track.isMute }, track.isMute, false) {
                            Icon(if (track.isMute) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, TRACK_ITEM_ICON_SIZE)
                        }
                        SegmentedDivider()
                        SegmentedButton({ track.isSolo = !track.isSolo }, track.isSolo, false) {
                            Icon(Crown, null, TRACK_ITEM_ICON_SIZE)
                        }
                        SegmentedDivider()
                        SegmentedButton({ track.isDisabled = !track.isDisabled }, track.isDisabled, false) {
                            Icon(DebugStepOver, null, TRACK_ITEM_ICON_SIZE)
                        }
                    }
                    if (trackHeight.value > 40) VolumeSlider(track, Modifier.fillMaxWidth().offset((-4).dp), false)
                }
            }

            Canvas(Modifier.fillMaxHeight().width(5.dp)) {
                val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
                drawRect(primaryColor, Offset(0F, y), Size(size.width, size.height - y))
                // drawLine(outlineColor, Offset(size.width / 2, y), Offset(size.width / 2, size.height), 0.5F)
                // drawLine(outlineColor, Offset.Zero, Offset(0F, size.height), 0.5F)
            }
        }
        track.subTracks.fastForEachIndexed { i, it -> key(it.id) {
            Divider(Modifier.offset(8.dp * (depth + 1)))
            TrackItem(playlist, it, track, i + 1, depth + 1)
        } }
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
        }.verticalScroll(verticalScrollState)) {
            Divider()
            val bus = EchoInMirror.bus!!
            bus.subTracks.fastForEachIndexed { i, it ->
                key(it.id) {
                    TrackItem(playlist, it, bus, i + 1)
                    Divider()
                }
            }
            TextButton({
                runBlocking {
                    TrackManager.instance.createTrack().doAddOrRemoveTrackAction(EchoInMirror.bus!!.subTracks)
                }
            }, Modifier.fillMaxWidth()) {
                Text("创建轨道")
            }
        }
    }
}
