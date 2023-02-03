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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastAll
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopeType
import com.eimsound.daw.components.utils.Stroke1PX
import com.eimsound.daw.components.utils.Stroke2PX
import com.eimsound.daw.utils.binarySearch
import com.eimsound.daw.utils.coerceIn
import com.eimsound.daw.utils.mapValue
import com.eimsound.daw.utils.mutableStateSetOf
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

fun DrawScope.drawEnvelope(type: EnvelopeType, color: Color, tension: Float, x0: Float, x1: Float, value0: Float, value1: Float) {
    if (value0 == value1) {
        val y0True = (1 - value0) * size.height
        drawLine(color, Offset(x0, y0True), Offset(x1, y0True), 2F)
        return
    }
    when (type) {
        EnvelopeType.SMOOTH -> {
            val controlPoint1X: Float
            val controlPoint1Y: Float
            val controlPoint2X: Float
            val controlPoint2Y: Float
            if (tension > 0) {
                val dx = (x1 - x0) * tension
                controlPoint1X = x0 + dx
                controlPoint1Y = value0
                controlPoint2X = x0 + x1 - dx
                controlPoint2Y = value1
            } else {
                val dy = (value1 - value0).absoluteValue * -tension
                val y0 = value0 + value1 - dy
                val y1 = min(value0, value1) + dy
                controlPoint1X = x0
                controlPoint1Y = if (value0 > value1) y0 else y1
                controlPoint2X = x1
                controlPoint2Y = if (value0 > value1) y1 else y0
            }
            drawPath(Path().apply {
                val height = size.height
                moveTo(x0, (1 - value0) * height)
                cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X, (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
            }, color, style = Stroke2PX)
        }
        EnvelopeType.EXPONENTIAL -> {
            val controlPoint1X: Float
            val controlPoint1Y: Float
            val controlPoint2X: Float
            val controlPoint2Y: Float
            if (tension > 0) {
                controlPoint1X = x0
                controlPoint1Y = value0 + (value1 - value0) * tension
                controlPoint2X = x1 + (x0 - x1) * tension
                controlPoint2Y = value1
            } else {
                controlPoint1X = x0 + (x0 - x1) * tension
                controlPoint1Y = value0
                controlPoint2X = x1
                controlPoint2Y = value1 + (value1 - value0) * tension
            }
            drawPath(Path().apply {
                val height = size.height
                moveTo(x0, (1 - value0) * height)
                cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X, (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
            }, color, style = Stroke2PX)
        }
        EnvelopeType.SQUARE -> {
            val y0True = (1 - value0) * size.height
            drawLine(color, Offset(x0, y0True), Offset(x1 - 0.5F, y0True), 2F)
            drawLine(color, Offset(x1 - 0.5F, y0True), Offset(x1, (1 - value1) * size.height), 2F)
        }
    }
}

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

private var isSelection by mutableStateOf(false)

@Composable
fun EnvelopeEditor(points: EnvelopePointList, start: Float, color: Color, valueRange: IntRange, noteWidth: MutableState<Dp>) {
    val lastIndex = remember { intArrayOf(0) }
    val hoveredIndex = remember { mutableStateOf(-1) }
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
                            val tmpId = getSelectedPoint(event.changes[0].position, points, start, valueRange, noteWidth)
                            if (tmpId != hoveredIndex.value) hoveredIndex.value = tmpId
                            continue
                        }
                        PointerEventType.Press -> {
                            val x = event.changes[0].position.x
                            val y = event.changes[0].position.y
                            if (event.keyboardModifiers.isCtrlPressed || event.buttons.isForwardPressed) {
                                selectedPoints.clear()
                                selectionStartX.value = x
                                selectionStartY.value = y
                                selectedX.value = x
                                selectedY.value = y
                                isSelection = true
                                break
                            }
                            isSelection = false
                            // find the selected point
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
                    isSelection = false
                    return@awaitPointerEventScope
                }

                drag(drag.id) {
                    selectedX.value = it.position.x
                    selectedY.value = it.position.y
                    it.consume()
                }

                if (isSelection) {
                    val noteWidthPx = noteWidth.value.toPx()
                    val startX = min(selectionStartX.value, selectedX.value) / noteWidthPx
                    val startY = min(selectionStartY.value, selectedY.value)
                    val endX = max(selectionStartX.value, selectedX.value) / noteWidthPx
                    val endY = max(selectionStartY.value, selectedY.value)
                    selectedPoints.addAll(points.filter {
                        val y = size.height * (1 - mapValue(it.value, valueRange))
                        it.time >= start + startX && it.time <= start + endX && y >= startY && y <= endY
                    })
                } else {
                    // TODO
                }

                isSelection = false
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

            val startX = (cur.time - start) * noteWidthPx
            val endX = if (next == null) size.width else (next.time - start) * noteWidthPx
            drawEnvelope(cur.type, color, cur.tension, startX, endX,
                mapValue(cur.value, valueRange), mapValue((next ?: cur).value, valueRange))
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
