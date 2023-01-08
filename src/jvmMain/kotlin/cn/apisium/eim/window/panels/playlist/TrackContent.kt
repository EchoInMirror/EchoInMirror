@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package cn.apisium.eim.window.panels.playlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.data.getEditUnit
import cn.apisium.eim.utils.*
import kotlin.math.absoluteValue

internal var action by mutableStateOf(EditAction.NONE)

@OptIn(ExperimentalComposeUiApi::class)
private val RESIZE_HAND_MODIFIER = Modifier.width(4.dp).fillMaxHeight()
    .pointerHoverIcon(PointerIconDefaults.HorizontalResize)

internal val selectedClips = mutableStateSetOf<TrackClip<*>>()
internal var deltaX by mutableStateOf(0)
internal var deltaY by mutableStateOf(0)
internal var trackHeights = ArrayList<TrackToHeight>()
private var densityGlobal by mutableStateOf(0F)
private val deletionList = mutableStateSetOf<TrackClip<*>>()

internal var selectionX by mutableStateOf(0F)
internal var selectionY by mutableStateOf(0F)
internal var selectionStartX = 0F
internal var selectionStartY = 0F

private suspend fun AwaitPointerEventScope.handleDragEvent(clip: TrackClip<*>, index: Int, track: Track) {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Final)
        if (event.buttons.isPrimaryPressed) {
            EchoInMirror.selectedTrack = track
            EchoInMirror.selectedClip = clip
            // TODO: add new clip
        } else if (event.buttons.isTertiaryPressed) {
            selectedClips.clear()
            deletionList.add(clip)
            action = EditAction.DELETE
            break
        } else if (event.buttons.isSecondaryPressed) {
            selectedClips.clear()
            action = EditAction.NONE
            break
        } else continue
    } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
    val down = event.changes[0]
    awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false) { change, _ ->
        trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
        densityGlobal = this.density
        selectedClips.add(clip)
        drag(down.id) {
            val currentY = dragStartY + it.position.y - change.position.y
            val cur = binarySearchTrackByHeight(trackHeights, currentY)
            deltaY = cur - index
            deltaX = ((it.position.x - change.position.x) / noteWidth.value.toPx()).fitInUnit(getEditUnit())
        }
        deltaY = 0
        deltaX = 0
        trackHeights.clear()
        if (action == EditAction.DELETE) {
            deletionList.clear()
        }
    }
}

@Suppress("DuplicatedCode")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TrackContent(track: Track, index: Int): Int {
    Box(Modifier.fillMaxWidth().height(trackHeight)) {
        track.clips.forEach {
            Box {
                val isSelected = selectedClips.contains(it)
                Box(Modifier
                    .pointerInput(track, index) { forEachGesture {
                        awaitPointerEventScope { handleDragEvent(it, index, track) } }
                    }
                    .run {
                        if (isSelected) {
                            absoluteOffset(noteWidth.value * deltaX,
                                if (deltaY == 0) Dp.Zero
                                else ((trackHeights[deltaY + index].height - trackHeights[index].height) / densityGlobal).dp
                            )
                        } else this
                    }
                ) {
                    if (!deletionList.contains(it)) {
                        val anim by animateFloatAsState(if (isSelected) 0.4F else 0.1F)
                        Box(
                            Modifier
                                .size(noteWidth.value * it.duration, trackHeight)
                                .absoluteOffset(noteWidth.value * it.time)
                                .background(track.color.copy(anim))
                                .border((anim * 5).dp, track.color, MaterialTheme.shapes.extraSmall)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .pointerHoverIcon(action.toPointerIcon(PointerIconDefaults.Hand))
                        ) {
                            @Suppress("TYPE_MISMATCH")
                            it.clip.factory.playlistContent(it.clip, track, trackHeight, noteWidth)
                        }
                        Spacer(RESIZE_HAND_MODIFIER)
                        Spacer(RESIZE_HAND_MODIFIER.align(Alignment.TopEnd))
                    }
                }
            }
        }
    }
    Divider()
    var i = index + 1
    track.subTracks.forEach { i += TrackContent(it, i) }
    return i
}

@Composable
internal fun TrackSelection(density: Density) {
    val scrollX = horizontalScrollState.value
    val scrollY = verticalScrollState.value
    val primaryColor = MaterialTheme.colorScheme.primary
    with (density) {
        Spacer(
            Modifier.size(
                (selectionX.coerceAtLeast(scrollX.toFloat()) - selectionStartX).absoluteValue.toDp(),
                (selectionY.coerceAtLeast(scrollY.toFloat()) - selectionStartY).absoluteValue.toDp()
            ).offset(
                (selectionStartX.coerceAtMost(selectionX) - scrollX).coerceAtLeast(0F).toDp(),
                (selectionStartY.coerceAtMost(selectionY) - scrollY).coerceAtLeast(0F).toDp()
            ).background(primaryColor.copy(0.1F)).border(1.dp, primaryColor)
        )
    }
}
