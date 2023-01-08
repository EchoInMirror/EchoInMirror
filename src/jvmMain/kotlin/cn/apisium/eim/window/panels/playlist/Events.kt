package cn.apisium.eim.window.panels.playlist

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import cn.apisium.eim.api.TrackClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal var dragStartY = 0F

@Suppress("DuplicatedCode")
internal suspend fun PointerInputScope.handleMouseEvent(scope: CoroutineScope) {
    forEachGesture {
        awaitPointerEventScope {
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
                            break
                        } else if (event.buttons.isForwardPressed) {
                            action = EditAction.SELECT
                            break
                        }
                    }
                    else -> {}
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]
            val downX = down.position.x + horizontalScrollState.value
            val downY = down.position.y + verticalScrollState.value

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) { change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) return@awaitPointerEventScope

            if (action == EditAction.SELECT) {
                selectedClips.clear()
                selectionStartX = downX
                selectionStartY = downY
                selectionX = selectionStartX
                selectionY = selectionStartY
            }

            dragStartY = down.position.y + verticalScrollState.value

            drag(drag.id) {
                when (action) {
                    EditAction.SELECT -> {
                        selectionX = (it.position.x.coerceAtMost(size.width.toFloat()) + horizontalScrollState.value)
                            .coerceAtLeast(0F)
                        selectionY = (it.position.y.coerceAtMost(size.height.toFloat()) + verticalScrollState.value)
                            .coerceAtLeast(0F)
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

                    val trackHeights = getAllTrackHeights(trackHeight.toPx(), density)
                    val cur = binarySearchTrackByHeight(trackHeights, minY)
                    val list = arrayListOf<TrackClip<*>>()
                    for (i in cur..trackHeights.lastIndex) {
                        val track = trackHeights[i].track
                        val height = trackHeights[i].height
                        for (j in track.clips.indices) {
                            val clip = track.clips[j]
                            if (clip.time + clip.duration < minX) continue
                            if (clip.time > maxX) break
                            if (clip.time <= minX || (clip.time <= maxX && maxX <= clip.time + clip.duration)) list.add(clip)
                        }
                        if (height > maxY) break
                    }
                    selectedClips.addAll(list)

                    selectionStartY = 0F
                    selectionStartX = 0F
                    selectionX = 0F
                    selectionY = 0F
                }
                else -> { }
            }
            action = EditAction.NONE
        }
    }
}
