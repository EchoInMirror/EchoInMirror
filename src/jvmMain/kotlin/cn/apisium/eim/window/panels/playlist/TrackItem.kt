@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package cn.apisium.eim.window.panels.playlist

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doAddOrRemoveTrackAction
import cn.apisium.eim.actions.doReorderAction
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.components.*
import cn.apisium.eim.components.icons.Crown
import cn.apisium.eim.components.icons.DebugStepOver
import cn.apisium.eim.utils.clickableWithIcon
import cn.apisium.eim.window.dialogs.openColorPicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.ArrayList

private val TRACK_ITEM_ICON_SIZE = Modifier.size(16.dp)
private val TRACK_COLOR_WIDTH = 8.dp

private class TrackMoveFlags(val parent: Track) {
    var isAbove by mutableStateOf(false)
    var isChild by mutableStateOf(false)
}
private val trackMovingFlags = WeakHashMap<Track, TrackMoveFlags>()
private data class TrackToHeight(val track: Track, val height: Float)
private fun getTrackHeights(list: ArrayList<TrackToHeight>, track: Track, defaultHeight: Float) {
    list.add(TrackToHeight(track, (list.lastOrNull()?.height ?: 0F) +
            (if (track.height == 0) defaultHeight else track.height.toFloat())))
    track.subTracks.forEach { getTrackHeights(list, it, defaultHeight) }
}

private var trackItemDragStartY = 0F

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TrackItem(track: Track, parentTrack: Track, index: Int, depth: Int = 0) {
    var isDragging by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val flags = trackMovingFlags[track]
    val alpha by animateFloatAsState(flags?.let { if (it.isAbove || it.isChild) 0.4F else 0F } ?: 0F)
    Row(Modifier.height(trackHeight).padding(start = TRACK_COLOR_WIDTH * depth).onPointerEvent(PointerEventType.Press) {
        EchoInMirror.selectedTrack = track
    }.drawWithContent {
        drawContent()
        if (flags == null || !(flags.isChild || flags.isAbove)) return@drawWithContent
        val left = TRACK_COLOR_WIDTH.toPx()
        if (flags.isChild) drawRect(primaryColor, Offset(left, 0F), Size(size.width - left, size.height), alpha = alpha)
        else if (flags.isAbove) drawRect(primaryColor, Offset(left, 0F), Size(size.width - left, 2F), alpha = alpha)
    }.pointerInput(track, parentTrack) {
        trackMovingFlags[track] = TrackMoveFlags(parentTrack)
        val defaultHeight = trackHeight.toPx()
        forEachGesture {
            awaitPointerEventScope {
                val down = awaitFirstDownOnPass(PointerEventPass.Final, false)
                awaitPointerSlopOrCancellation(
                    down.id,
                    down.type,
                    triggerOnMainAxisSlop = false
                ) { change, _ ->
                    val trackHeights: ArrayList<TrackToHeight> = ArrayList()
                    EchoInMirror.bus!!.subTracks.forEach { getTrackHeights(trackHeights, it, defaultHeight) }
                    isDragging = true
                    var lastTrack: Track? = null
                    var lastFlags: TrackMoveFlags? = null
                    var currentY: Float
                    drag(down.id) {
                        currentY = trackItemDragStartY + it.position.y - change.position.y
                        // binary search drop track by trackHeights and currentY
                        var l = 0
                        var r = trackHeights.size - 1
                        while (l < r) {
                            val mid = (l + r) / 2
                            if (trackHeights[mid].height < currentY) l = mid + 1
                            else r = mid
                        }
                        val dropTrack = trackHeights[l].track
                        lastFlags?.isAbove = false
                        lastFlags?.isChild = false
                        if (dropTrack == track) return@drag
                        lastTrack = dropTrack
                        val flags0 = trackMovingFlags[dropTrack] ?: return@drag
                        lastFlags = flags0
                        if (currentY - (if (l == 0) 0F else trackHeights[l - 1].height) < 6) flags0.isAbove = true
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
                            if (it.isAbove) track.doReorderAction(parentTrack.subTracks, it.parent.subTracks,
                                it.parent.subTracks.indexOf(lastTrack))
                            else if (it.isChild) track.doReorderAction(parentTrack.subTracks, lastTrack!!.subTracks)
                        }
                    }
                    lastFlags?.isAbove = false
                    lastFlags?.isChild = false
                    isDragging = false
                }
            }
        }
    }.alpha(animateFloatAsState(if (isDragging) 0.3F else 1F).value)) {
//        Box {
//            val color by animateColorAsState(track.color, tween(100))
//            Canvas(Modifier.fillMaxHeight().width(8.dp).background(color.copy(alpha = 0.5F)).clickableWithIcon {
//                openColorPicker(track.color) { track.color = it }
//            }) {
//                val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
//                drawRect(color, Offset(0F, y), Size(size.width, size.height - y))
//            }
//        }
        val localFloatingDialogProvider = LocalFloatingDialogProvider.current
        Spacer(Modifier.fillMaxHeight().width(8.dp).background(track.color).clickableWithIcon {
            openColorPicker(localFloatingDialogProvider, track.color) { track.color = it }
        })
        Row(Modifier.padding(8.dp, 4.dp)) {
            Text(index.toString(),
                Modifier.width(20.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Column(Modifier.fillMaxHeight(), Arrangement.SpaceBetween) {
                Text(track.name,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
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
    }
    track.subTracks.forEachIndexed { i, it -> key(it.id) {
        Divider(Modifier.offset(8.dp * (depth + 1)))
        TrackItem(it, track, i + 1, depth + 1)
    } }
}

@Composable
internal fun TrackItems() {
    val scope = rememberCoroutineScope()
    Column(Modifier.pointerInput(scope) {
        detectDragGestures({ trackItemDragStartY = it.y + verticalScrollState.value }) { change, _ ->
            if (change.position.y < 10) scope.launch { verticalScrollState.scrollBy(-3F) }
            else if (change.position.y > size.height - 10) scope.launch { verticalScrollState.scrollBy(3F) }
        }
    }.verticalScroll(verticalScrollState)) {
        Divider()
        val bus = EchoInMirror.bus!!
        bus.subTracks.forEachIndexed { i, it ->
            key(it.id) {
                TrackItem(it, bus, i + 1)
                Divider()
            }
        }
        TextButton({
            runBlocking {
                EchoInMirror.audioProcessorManager.createTrack().doAddOrRemoveTrackAction(EchoInMirror.bus!!.subTracks)
            }
        }, Modifier.fillMaxWidth()) {
            Text("创建轨道")
        }
    }
}