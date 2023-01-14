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
import cn.apisium.eim.actions.doClipsAmountAction
import cn.apisium.eim.actions.doClipsEditActionAction
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.defaultMidiClipFactory
import cn.apisium.eim.api.oneBarPPQ
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
internal val deletionList = mutableStateSetOf<TrackClip<*>>()

internal var selectionX by mutableStateOf(0F)
internal var selectionY by mutableStateOf(0F)
internal var selectionStartX = 0F
internal var selectionStartY = 0F
internal var resizeDirectionRight = false

private suspend fun AwaitPointerEventScope.handleDragEvent(clip: TrackClip<*>, index: Int, track: Track) {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.type == PointerEventType.Press) {
            if (event.buttons.isPrimaryPressed) {
                EchoInMirror.selectedTrack = track
                EchoInMirror.selectedClip = clip
                if (!selectedClips.contains(clip)) {
                    selectedClips.clear()
                    selectedClips.add(clip)
                }
            } else if (event.buttons.isSecondaryPressed) {
                selectedClips.clear()
                action = EditAction.NONE
                break
            } else if (event.buttons.isTertiaryPressed) {
                deletionList.add(clip)
                continue
            }
        }
    } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
    val down = event.changes[0]
    awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false) { change, _ ->
        var yAllowRange: IntRange? = null
        var minClipDuration = Int.MAX_VALUE
        var selectedClipsLeft = Int.MAX_VALUE
        var trackToIndex: HashMap<Track, Int>? = null
        var allowExpandableLeft = -Int.MAX_VALUE
        var allowExpandableRight = Int.MAX_VALUE
        if (event.buttons.isPrimaryPressed) {
            val fourDp = 4 * density
            val rdr = change.position.x > size.width - fourDp
            resizeDirectionRight = rdr
            action = if (change.position.x < fourDp || rdr) {
                selectedClips.forEach {
                    val left = it.time
                    if (left < selectedClipsLeft) selectedClipsLeft = left
                    val duration = it.duration
                    if (!it.clip.isExpandable) {
                        if (allowExpandableLeft < it.start) allowExpandableLeft = it.start
                        if (it.clip.maxDuration > 0 && it.clip.maxDuration - duration < allowExpandableRight)
                            allowExpandableRight = it.clip.maxDuration - duration
                    }
                    if (duration < minClipDuration) minClipDuration = duration
                }
                EditAction.RESIZE
            } else {
                trackToIndex = hashMapOf()
                trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
                trackHeights.forEachIndexed { index, (track) -> trackToIndex[track] = index }
                var selectedClipsTop = Int.MAX_VALUE
                var selectedClipsBottom = 0
                selectedClips.forEach {
                    val top = trackToIndex[it.track] ?: return
                    val left = it.time
                    if (top < selectedClipsTop) selectedClipsTop = top
                    if (top > selectedClipsBottom) selectedClipsBottom = top
                    if (left < selectedClipsLeft) selectedClipsLeft = left
                }
                yAllowRange = -selectedClipsTop..(trackHeights.size - selectedClipsBottom)
                EditAction.MOVE
            }
        }
        when (action) {
            EditAction.MOVE -> {
                change.consume()
                drag(down.id) {
                    val currentY = dragStartY + it.position.y - change.position.y
                    val cur = binarySearchTrackByHeight(trackHeights, currentY)
                    deltaY = (cur - index).coerceIn(yAllowRange!!)
                    deltaX = ((it.position.x - change.position.x) / noteWidth.value.toPx()).fitInUnit(getEditUnit())
                        .coerceAtLeast(-selectedClipsLeft)
                    it.consume()
                }
                if (deltaX != 0 || deltaY != 0) {
                    action = EditAction.NONE
                    val y = deltaY
                    val x = deltaX
                    deltaY = 0
                    deltaX = 0
                    doClipsEditActionAction(selectedClips.toList(), x, 0, 0,
                        if (y == 0) null
                        else try {
                            selectedClips.map { trackHeights[trackToIndex!![it.track]!! + y].track }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            null
                        }
                    )
                }
                trackHeights.clear()
            }
            EditAction.RESIZE -> {
                change.consume()
                drag(down.id) {
                    val noteUnit = getEditUnit()
                    var x = ((it.position.x - change.position.x) / noteWidth.value.toPx()).fitInUnit(noteUnit)
                    if (resizeDirectionRight) {
                        if (minClipDuration + x < noteUnit) x = noteUnit - minClipDuration
                        if (allowExpandableRight < x) x = allowExpandableRight
                    } else {
                        if (x < -selectedClipsLeft) x = -selectedClipsLeft
                        if (-allowExpandableLeft > x) x = -allowExpandableLeft
                    }
                    if (x != deltaX) deltaX = x
                    it.consume()
                }
                if (deltaX != 0) {
                    action = EditAction.NONE
                    val x = deltaX
                    deltaX = 0
                    if (resizeDirectionRight) doClipsEditActionAction(selectedClips.toList(), deltaDuration = x)
                    else doClipsEditActionAction(selectedClips.toList(), x, -x, x)
                }
            }
            else -> { }
        }
    }
}

@Suppress("DuplicatedCode")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TrackContent(track: Track, index: Int, density: Density): Int {
    Box(Modifier.fillMaxWidth().height(trackHeight)) {
        Box(Modifier.fillMaxSize().pointerInput(track) {
            detectTapGestures(onDoubleTap = {
                val len = EchoInMirror.currentPosition.oneBarPPQ
                val clip = EchoInMirror.clipManager.createTrackClip(
                    EchoInMirror.clipManager.defaultMidiClipFactory.createClip(),
                    (it.x / noteWidth.value.toPx()).fitInUnit(len),
                    len,
                    0,
                    track
                )
                doClipsAmountAction(listOf(clip), false)
            })
        })
        track.clips.read()
        track.clips.forEach {
            key(it) {
                Box {
                    with (density) {
                        val isSelected = selectedClips.contains(it)
                        val scrollXPPQ = horizontalScrollState.value / noteWidth.value.toPx()
                        val contentPPQ = contentWidth.value / noteWidth.value.value
                        if (it.time <= scrollXPPQ + contentPPQ && it.time + it.duration >= scrollXPPQ) {
                            val clipStartPPQ = it.time
                            val clipEndPPQ = clipStartPPQ + it.duration
                            var clipStartPPQOnMove = clipStartPPQ
                            var clipEndPPQOnMove = clipEndPPQ
                            var y = Dp.Zero
                            if (isSelected) {
                                if (action == EditAction.MOVE) {
                                    clipStartPPQOnMove += deltaX
                                    clipEndPPQOnMove += deltaX
                                    if (deltaY != 0) y = (trackHeights[(deltaY + index).coerceAtMost(trackHeights.size - 1)]
                                        .height - trackHeights[index].height).toDp()
                                } else if (action == EditAction.RESIZE) {
                                    if (resizeDirectionRight) clipEndPPQOnMove += deltaX
                                    else clipStartPPQOnMove += deltaX
                                }
                            }
                            val widthInPPQ = clipEndPPQOnMove.toFloat().coerceAtMost(scrollXPPQ + contentPPQ) -
                                    clipStartPPQOnMove.toFloat().coerceAtLeast(scrollXPPQ)
                            Box(Modifier
                                .absoluteOffset(noteWidth.value * (clipStartPPQ - scrollXPPQ).coerceAtLeast(0F))
                                .pointerInput(it, track, index) {
                                    forEachGesture { awaitPointerEventScope { handleDragEvent(it, index, track) } }
                                }
                                .size(noteWidth.value * widthInPPQ, trackHeight)
                                .run {
                                    if (clipStartPPQ != clipStartPPQOnMove) {
                                        absoluteOffset(noteWidth.value * (
                                                if (clipStartPPQOnMove < scrollXPPQ) {
                                                    if (deltaX < 0) -(clipStartPPQ - scrollXPPQ).coerceAtLeast(0F) else 0F
                                                } else clipStartPPQOnMove - clipStartPPQ.toFloat().coerceAtLeast(scrollXPPQ)), y)
                                    } else if (y.value != 0F) absoluteOffset(Dp.Zero, y)
                                    else this
                                }
                            ) {
                                if (!deletionList.contains(it)) {
                                    val trackColor = track.color.copy(0.85F)
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(trackColor, MaterialTheme.shapes.extraSmall)
                                            .run {
                                                if (isSelected) border(
                                                    2.dp, MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.shapes.extraSmall
                                                )
                                                else this
                                            }
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .pointerHoverIcon(action.toPointerIcon(PointerIconDefaults.Hand))
                                    ) {
                                        @Suppress("TYPE_MISMATCH")
                                        it.clip.factory.playlistContent(
                                            it, track,
                                            trackColor.toOnSurfaceColor()
                                                .copy(animateFloatAsState(if (isSelected) 1F else 0.8F).value),
                                            trackHeight, noteWidth,
                                            (scrollXPPQ - clipStartPPQOnMove).coerceAtLeast(0F) +
                                                    (it.start + if (isSelected && action == EditAction.RESIZE &&
                                                        !resizeDirectionRight) deltaX else 0),
                                            widthInPPQ
                                        )
                                    }
                                    Spacer(RESIZE_HAND_MODIFIER)
                                    Spacer(RESIZE_HAND_MODIFIER.align(Alignment.TopEnd))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Divider()
    var i = index + 1
    track.subTracks.forEach { i += TrackContent(it, i, density) }
    return i - index
}

@Composable
internal fun TrackSelection(density: Density) {
    if (action != EditAction.SELECT) return
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
