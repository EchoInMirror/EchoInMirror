package com.eimsound.daw.window.panels.playlist

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.components.utils.EditAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal var dragStartY = 0F

@Suppress("DuplicatedCode")
internal suspend fun PointerInputScope.handleMouseEvent(playlist: Playlist, scope: CoroutineScope) {
    awaitEachGesture {
        playlist.apply {
            var event: PointerEvent
            do {
                event = awaitPointerEvent(PointerEventPass.Initial)
                when (event.type) {
                    PointerEventType.Scroll -> {
                        calcScroll(event, noteWidth, horizontalScrollState, scope) {
                            val newValue = (trackHeight.value + (if (it.scrollDelta.y > 0) -2 else 2)).coerceIn(28F, 100F)
                            if (newValue == trackHeight.value) return@calcScroll
                            val y = it.position.x
                            val oldY = (y + verticalScrollState.value) / trackHeight.toPx()
                            trackHeight = newValue.dp
                            scope.launch {
                                val trackHeightPx = trackHeight.toPx()
                                verticalScrollState.scrollBy(
                                    (oldY - (y + verticalScrollState.value) / trackHeightPx) * trackHeightPx
                                )
                            }
                        }
                        continue
                    }
                    PointerEventType.Press -> {
                        if (event.buttons.isPrimaryPressed) {
                            if (event.keyboardModifiers.isCtrlPressed) action = EditAction.SELECT
                            selectedClips.clear()
                            break
                        } else if (event.buttons.isForwardPressed) {
                            selectedClips.clear()
                            action = EditAction.SELECT
                            break
                        } else if (event.buttons.isTertiaryPressed) {
                            selectedClips.clear()
                            action = EditAction.DELETE
                            break
                        }
                    }
                    else -> {}
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]
            val downX = down.position.x + horizontalScrollState.value
            dragStartY = down.position.y + verticalScrollState.value

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) { change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) return@awaitEachGesture

            var trackHeights: List<TrackToHeight>? = null
            when (action) {
                EditAction.SELECT -> {
                    selectionStartX = downX
                    selectionStartY = dragStartY
                    selectionX = downX
                    selectionY = dragStartY
                }
                EditAction.DELETE -> {
                    trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
                }
                else -> { }
            }

            drag(down.id) {
                when (action) {
                    EditAction.SELECT -> {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value)
                            .coerceAtLeast(0F)
                        selectionY = (it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value)
                            .coerceAtLeast(0F)
                    }
                    EditAction.DELETE -> {
                        val y = it.position.y + verticalScrollState.value
                        val x = it.position.x + horizontalScrollState.value / noteWidth.value.toPx()
                        val heights = trackHeights!!
                        val track = heights[binarySearchTrackByHeight(heights, y)].track
                        for (j in track.clips.indices) {
                            val clip = track.clips[j]
                            if (clip.time <= x && x <= clip.time + clip.duration) deletionList.add(clip)
                        }
                    }
                    else -> {}
                }
                if (it.position.y < 10) scope.launch { verticalScrollState.scrollBy(-3F) }
                else if (it.position.y > size.height - 10) scope.launch { verticalScrollState.scrollBy(3F) }
                if (it.position.x < 10) scope.launch { horizontalScrollState.scrollBy(-3F) }
                else if (it.position.x > size.width - 10) scope.launch { horizontalScrollState.scrollBy(3F) }
                it.consume()
            }
            when (action) {
                EditAction.SELECT -> {
                    val startX = (selectionStartX / noteWidth.value.toPx()).roundToInt()
                    val endX = (selectionX / noteWidth.value.toPx()).roundToInt()
                    val minX = minOf(startX, endX)
                    val maxX = maxOf(startX, endX)
                    val minY = minOf(selectionStartY, selectionY)
                    val maxY = maxOf(selectionStartY, selectionY)

                    trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
                    val cur = binarySearchTrackByHeight(trackHeights, minY)
                    val list = arrayListOf<TrackClip<*>>()
                    for (i in cur..trackHeights.lastIndex) {
                        val track = trackHeights[i].track
                        val height = trackHeights[i].height
                        for (j in track.clips.indices) {
                            val clip = track.clips[j]
                            val clipStart = clip.time
                            val clipEnd = clipStart + clip.duration
                            if (clipEnd < minX) continue
                            if (clipStart > maxX) break
                            if (
                                clipStart in minX..maxX || clipEnd in minX..maxX ||
                                minX in clipStart..clipEnd || maxX in clipStart..clipEnd
                            ) list.add(clip)
                        }
                        if (height > maxY) break
                    }
                    selectedClips.addAll(list)

                    selectionStartY = 0F
                    selectionStartX = 0F
                    selectionX = 0F
                    selectionY = 0F
                }
                EditAction.DELETE -> deletionList.clear()
                else -> { }
            }
            action = EditAction.NONE
        }
    }
}
