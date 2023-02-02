package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastAll
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.daw.components.utils.Stroke1PX
import com.eimsound.daw.components.utils.Stroke2PX
import com.eimsound.daw.utils.binarySearch
import com.eimsound.daw.utils.coerceIn
import com.eimsound.daw.utils.mutableStateSetOf
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

private fun AwaitPointerEventScope.getSelectedPoint(position: Offset, points: EnvelopePointList, start: Float,
                                                    valueRange: IntRange, noteWidth: MutableState<Dp>): Int {
    val noteWidthPx = noteWidth.value.toPx()
    val targetX = start + position.x / noteWidthPx
    val pointIndex = points.binarySearch { it.time < targetX }
    val point = points[pointIndex]
    val range = valueRange.last - valueRange.first
    return if ((point.time - targetX).absoluteValue < 8F / noteWidthPx &&
        (size.height * (1 - point.value.coerceIn(valueRange) / range) - position.y).absoluteValue < 8) pointIndex else -1
}

private var isSelection = false

@Composable
fun EnvelopeEditor(points: EnvelopePointList, start: Float, color: Color, valueRange: IntRange, noteWidth: MutableState<Dp>) {
    val lastIndex = remember { intArrayOf(0) }
    var hoveredIndex = remember { mutableStateOf(-1) }
    val selectionStartX = remember { mutableStateOf(0F) }
    val selectionStartY = remember { mutableStateOf(0F) }
    val selectedX = remember { mutableStateOf(0F) }
    val selectedY = remember { mutableStateOf(0F) }
    val selectedPoints = remember { mutableStateSetOf<EnvelopePoint>() }
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(Modifier.fillMaxSize().pointerInput(points, lastIndex, valueRange) {
        forEachGesture {
            awaitPointerEventScope {
                var event: PointerEvent
                do {
                    event = awaitPointerEvent(PointerEventPass.Main)
                    when (event.type) {
                        PointerEventType.Move -> {
                            // find the hovered point
                            val x = event.changes[0].position.x
                            val y = event.changes[0].position.y
                            val tmpId = getSelectedPoint(event.changes[0].position, points, start, valueRange, noteWidth)
                            if (tmpId != hoveredIndex.value) hoveredIndex.value = tmpId
                            continue
                        }
                        PointerEventType.Press -> {
                            if (event.keyboardModifiers.isCtrlPressed || event.buttons.isForwardPressed) {
                                isSelection = true
                                break
                            }
                            isSelection = false
                            // find the selected point
                            val x = event.changes[0].position.x
                            val y = event.changes[0].position.y
                            val tmpId = getSelectedPoint(event.changes[0].position, points, start, valueRange, noteWidth)
                            if (tmpId != -1) selectedPoints.add(points[tmpId])
                            continue
                        }
                    }
                } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
                val down = event.changes[0]
//            val downX = down.position.x + horizontalScrollState.value
//            val downY = down.position.y + verticalScrollState.value

                var drag: PointerInputChange?
                do {
                    @Suppress("INVISIBLE_MEMBER")
                    drag = awaitPointerSlopOrCancellation(
                        down.id, down.type,
                        triggerOnMainAxisSlop = false
                    ) { change, _ -> change.consume() }
                } while (drag != null && !drag.isConsumed)
                if (drag == null) {
                    return@awaitPointerEventScope
                }

                if (isSelection) {
                    selectionStartX.value = down.position.x
                    selectionStartY.value = down.position.y
                }

                drag(drag.id) {
                    if (isSelection) {
                        selectedX.value = it.position.x
                        selectedY.value = it.position.y
//                        selectedPoints.clear()
//                        selectedPoints.addAll(points.filter { it.time >= start + selectionStartX.value / noteWidth.value.toPx() &&
//                                it.time <= start + it.time / noteWidth.value.toPx() })
                    } else {
//                        selectedX.value = down.position.x
//                        selectedY.value = down.position.y
                    }
                    it.consume()
                }

                selectionStartX.value = 0F
                selectionStartY.value = 0F
                selectedX.value = 0F
                selectedX.value = 0F
            }
        }
    }) {
        val noteWidthPx = noteWidth.value.toPx()
        val end = start + size.width / noteWidthPx
        // binary search for start point
        points.read()
        val startIdx = (points.binarySearch { it.time < start } - 1).coerceAtLeast(0)
        lastIndex[0] = startIdx
        val range = valueRange.last - valueRange.first

        // draw line
        for (i in startIdx until points.size) {
            val cur = points[i]
            if (cur.time > end) break
            val next = points.getOrNull(i + 1)

            drawPath(Path().apply {
                val startX = (cur.time - start) * noteWidthPx
                val startY = size.height * (1 - cur.value.coerceIn(valueRange) / range)
                val endX = if (next == null) size.width else (next.time - start) * noteWidthPx
                val endY = size.height * (1 - (next ?: cur).value.coerceIn(valueRange) / range)
                val tension = cur.tension - 0.5F
                // get control point by exponential function
                val controlX = (endX - startX) * 0.5F + startX + tension * (endY - startY)
                val controlY = (endY - startY) * 0.5F + startY + tension * (startX - endX)
                moveTo(startX, startY)
                quadraticBezierTo(controlX, controlY, endX, endY)

            }, color, style = Stroke2PX)

            
        }

        // draw points
        val selectedId = hoveredIndex.value
        for (i in startIdx until points.size) {
            val cur = points[i]
            if (cur.time > end) break
            drawCircle(if (selectedPoints.contains(cur)) primaryColor else color, if (selectedId == i) 8F else 4F,
                Offset(
                    (cur.time - start) * noteWidthPx,
                    size.height * (1 - cur.value.coerceIn(valueRange) / range)
                )
            )
        }

        // draw selection area
        if (isSelection) {
            val startX = min(selectionStartX.value, selectedX.value)
            val startY = min(selectionStartY.value, selectedY.value)
            val endX = max(selectionStartX.value, selectedX.value)
            val endY = max(selectionStartY.value, selectedY.value)
            val pos = Offset(startX, startY)
            val size = Size(endX - startX, endY - startY)
            drawRect(primaryColor.copy(0.1F), pos, size)
            drawRect(primaryColor, pos, size, style = Stroke1PX)
        }
    }
}
