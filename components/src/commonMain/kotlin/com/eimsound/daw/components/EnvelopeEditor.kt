package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastAll
import com.eimsound.audioprocessor.data.EnvelopePoint
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.audioprocessor.data.EnvelopeType
import com.eimsound.audioprocessor.data.SerializableEnvelopePointList
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.Stroke1PX
import com.eimsound.daw.components.utils.Stroke2PX
import com.eimsound.daw.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.launch
import kotlin.math.*

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
                controlPoint2X = x1 - dx
                controlPoint2Y = value1
            } else {
                val dy = (value1 - value0).absoluteValue * -tension
                if (value0 > value1) {
                    controlPoint1Y = value0 - dy
                    controlPoint2Y = value1 + dy
                } else {
                    controlPoint1Y = value0 + dy
                    controlPoint2Y = value1 - dy
                }
                controlPoint1X = x0
                controlPoint2X = x1
            }
            drawPath(Path().apply {
                val height = size.height
                moveTo(x0, (1 - value0) * height)
                cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X,
                    (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
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
                cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X,
                    (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
            }, color, style = Stroke2PX)
        }
        EnvelopeType.SQUARE -> {
            val y0True = (1 - value0) * size.height
            drawLine(color, Offset(x0, y0True), Offset(x1 - 0.5F, y0True), 2F)
            drawLine(color, Offset(x1 - 0.5F, y0True), Offset(x1, (1 - value1) * size.height), 2F)
        }
    }
}

private fun PointerInputScope.getSelectedPoint(position: Offset, points: EnvelopePointList, start: Float,
                                                    valueRange: IntRange, noteWidth: MutableState<Dp>): Pair<Int, Int> {
    val noteWidthPx = noteWidth.value.toPx()
    val targetX = start + position.x / noteWidthPx
    val pointIndex = points.binarySearch { it.time <= targetX }
    val point = points[pointIndex]
    fun checkIsSelectedPoint(point: EnvelopePoint?) = point != null && (point.time - targetX).absoluteValue < 8F / noteWidthPx &&
            (size.height * (1 - point.value.coerceIn(valueRange) / valueRange.range) - position.y).absoluteValue < 8
    return (if (point.time < targetX) pointIndex else pointIndex - 1) to if (checkIsSelectedPoint(point)) pointIndex else
        if (checkIsSelectedPoint(points.getOrNull(pointIndex - 1))) pointIndex - 1 else -1
}

interface EnvelopeEditorEventHandler {
    fun onAddPoints(editor: EnvelopeEditor, points: List<EnvelopePoint>)
    fun onPastePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>): List<EnvelopePoint>
    fun onRemovePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>)
    fun onMovePoints(editor: EnvelopeEditor, points: List<EnvelopePoint>, offsetTime: Int, offsetValue: Float)
    fun onTensionChanged(editor: EnvelopeEditor, points: List<EnvelopePoint>, tension: Float)
}

class EnvelopeEditor(val points: EnvelopePointList, val valueRange: IntRange, private val isDecimal: Boolean = false,
                     private val eventHandler: EnvelopeEditorEventHandler? = null): SerializableEditor {
    @Suppress("MemberVisibilityCanBePrivate")
    val selectedPoints = mutableStateSetOf<EnvelopePoint>()
    private var selectionStartX by mutableStateOf(0F)
    private var selectionStartY by mutableStateOf(0F)
    private var offsetX by mutableStateOf(0F)
    private var offsetY by mutableStateOf(0F)
    private var offsetTension by mutableStateOf(0F)
    private var action by mutableStateOf(EditAction.NONE)
    private var hoveredIndex by mutableStateOf(-1)
    private var startIndex = 0
    private var startValue = 0F
    private var clipStartTimeValue = 0
    private var editUnitValue = 0
    private var tempPoints: MutableList<EnvelopePoint>? = null
    private var currentAdjustingPoint: Int = -1

    companion object {
        var copiedPoints: List<EnvelopePoint>? = null
        var defaultTension = 0F
    }

    private fun copyAsObject(): List<EnvelopePoint> {
        val startTime = selectedPoints.minOf { it.time }
        return selectedPoints.map { it.copy(it.time - startTime, mapValue(it.value, valueRange)) }
    }

    override fun copy() {
        if (selectedPoints.isEmpty()) return
        copiedPoints = copyAsObject()
    }
    override fun paste() {
        val result = eventHandler?.onPastePoints(this, copiedPoints ?: return) ?: return
        selectedPoints.clear()
        selectedPoints.addAll(result)
    }
    override fun copyAsString(): String = if (selectedPoints.isEmpty()) "" else jacksonObjectMapper().writeValueAsString(
        SerializableEnvelopePointList(copyAsObject()))

    override fun pasteFromString(value: String) {
        try {
            val result = eventHandler?.onPastePoints(this,
                jacksonObjectMapper().readValue<SerializableEnvelopePointList>(value).points) ?: return
            selectedPoints.clear()
            selectedPoints.addAll(result)
        } catch (ignored: Throwable) { }
    }

    override fun delete() { eventHandler?.onRemovePoints(this, selectedPoints.toList()) }
    override fun selectAll() { selectedPoints.addAll(points) }

    @OptIn(ExperimentalTextApi::class)
    @Composable
    fun Editor(start: Float, color: Color, noteWidth: MutableState<Dp>, editUnit: Int = 24,
                horizontalScrollState: ScrollState? = null, clipStartTime: Int = 0) {
        val scope = rememberCoroutineScope()
        val measurer = rememberTextMeasurer(50)
        val primaryColor = MaterialTheme.colorScheme.primary
        val textStyle = MaterialTheme.typography.labelMedium.copy(color)
        startValue = start
        clipStartTimeValue = clipStartTime
        editUnitValue = editUnit

//        if (hoveredIndex == -1 && action == EditAction.RESIZE) modifier = modifier.pointerHoverIcon(PointerIconDefaults.VerticalResize)

        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onDoubleTap = {
                if (getSelectedPoint(it, points, startValue, valueRange, noteWidth).second != -1) return@detectTapGestures
                var newValue = (1 - it.y / size.height) * valueRange.range + valueRange.first
                if (!isDecimal) newValue = round(newValue)
                val targetX = startValue + it.x / noteWidth.value.toPx()
                val newPoint = EnvelopePoint(targetX.fitInUnit(editUnitValue), newValue.coerceIn(valueRange), defaultTension)
                eventHandler?.onAddPoints(this@EnvelopeEditor, listOf(newPoint))
            })
        }.pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Move -> {
                                // find the hovered point
                                val (_, tmpId) = getSelectedPoint(event.changes[0].position, points, startValue, valueRange, noteWidth)
                                if (tmpId != hoveredIndex) hoveredIndex = tmpId
                                continue
                            }
                            PointerEventType.Press -> {
                                val x = event.changes[0].position.x
                                val y = event.changes[0].position.y
                                if (event.keyboardModifiers.isCtrlPressed || event.buttons.isForwardPressed) {
                                    selectedPoints.clear()
                                    selectionStartX = x
                                    selectionStartY = y
                                    offsetX = x
                                    offsetY = y
                                    action = EditAction.SELECT
                                    break
                                }
                                // find the selected point
                                val (pointId, tmpId) = getSelectedPoint(event.changes[0].position, points,
                                    startValue, valueRange, noteWidth)
                                if (tmpId == -1) {
                                    if (pointId >= 0 && pointId < points.size - 1) {
                                        if (!selectedPoints.contains(points[pointId])) selectedPoints.clear()
                                        currentAdjustingPoint = pointId
                                        action = EditAction.RESIZE
                                        break
                                    }
                                } else {
                                    if (!selectedPoints.contains(points[tmpId])) {
                                        selectedPoints.clear()
                                        selectedPoints.add(points[tmpId])
                                    }
                                    action = EditAction.MOVE
                                    break
                                }
                                continue
                            }
                        }
                    } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
                    val down = event.changes[0]
                    val downX = down.position.x + (horizontalScrollState?.value?.toFloat() ?: 0F)
                    val downY = down.position.y

                    var drag: PointerInputChange?
                    do {
                        @Suppress("INVISIBLE_MEMBER")
                        drag = awaitPointerSlopOrCancellation(
                            down.id, down.type,
                            triggerOnMainAxisSlop = false
                        ) { change, _ -> change.consume() }
                    } while (drag != null && !drag.isConsumed)
                    if (drag == null) {
                        action = EditAction.NONE
                        return@awaitPointerEventScope
                    }
                    var selectedPointsLeft = Int.MAX_VALUE
//                    var selectedPointsRight = Int.MIN_VALUE
                    var selectedPointsTop = valueRange.first.toFloat()
                    var selectedPointsBottom = valueRange.last.toFloat()
                    if (action == EditAction.MOVE) {
                        selectedPoints.forEach {
                            if (it.time < selectedPointsLeft) selectedPointsLeft = it.time
//                            if (it.time > selectedPointsRight) selectedPointsRight = it.time
                            if (it.value > selectedPointsTop) selectedPointsTop = it.value
                            if (it.value < selectedPointsBottom) selectedPointsBottom = it.value
                        }
                        tempPoints = points.toMutableList()
                    }

                    drag(drag.id) {
                        var y = it.position.y
                        if (action == EditAction.RESIZE) {
                            y = -((y - downY) / size.height).coerceIn(-2F, 2F)
                            if (offsetTension != y) offsetTension = y
                        } else {
                            var x = it.position.x
                            if (action == EditAction.MOVE) {
                                y = -((y - downY) / size.height).coerceIn(-1F, 1F) * valueRange.range
                                if (!isDecimal) y = round(y)
                                x = (((x + (horizontalScrollState?.value?.toFloat() ?: 0F)).coerceAtLeast(0F) - downX) /
                                        noteWidth.value.toPx()).fitInUnit(editUnitValue).toFloat()
                                if (selectedPointsLeft + x + clipStartTimeValue < 0) x = -(selectedPointsLeft.toFloat() + clipStartTimeValue)
                                if (selectedPointsTop + y > valueRange.last) y = valueRange.last - selectedPointsTop
                                if (selectedPointsBottom + y < valueRange.first) y = valueRange.first - selectedPointsBottom
                                if (x != offsetX) {
                                    offsetX = x
                                    tempPoints?.sortBy { p -> p.time + (if (selectedPoints.contains(p)) x else 0F) }
                                }
                            } else if (x != offsetX) offsetX = x
                            if (y != offsetY) offsetY = y
                            if (horizontalScrollState != null) {
                                if (it.position.x < 10) scope.launch { horizontalScrollState.scrollBy(-3F) }
                                else if (it.position.x > size.width - 10) scope.launch { horizontalScrollState.scrollBy(3F) }
                            }
                        }
                        it.consume()
                    }

                    when (action) {
                        EditAction.SELECT -> {
                            val noteWidthPx = noteWidth.value.toPx()
                            val startX = min(selectionStartX, offsetX) / noteWidthPx
                            val startY = min(selectionStartY, offsetY)
                            val endX = max(selectionStartX, offsetX) / noteWidthPx
                            val endY = max(selectionStartY, offsetY)
                            selectedPoints.addAll(points.filter {
                                val y = size.height * (1 - mapValue(it.value, valueRange))
                                it.time >= start + startX && it.time <= start + endX && y >= startY && y <= endY
                            })
                        }
                        EditAction.MOVE ->
                            eventHandler?.onMovePoints(this@EnvelopeEditor, selectedPoints.toList(), offsetX.toInt(), offsetY)
                        EditAction.RESIZE -> {
                            val point = points[currentAdjustingPoint]
                            defaultTension = (point.tension + offsetTension).coerceIn(-1F, 1F)
                            eventHandler?.onTensionChanged(this@EnvelopeEditor, if (selectedPoints.isEmpty())
                                listOf(point) else selectedPoints.toList(), offsetTension)
                        }
                        else -> {}
                    }

                    action = EditAction.NONE
                    selectionStartX = 0F
                    selectionStartY = 0F
                    offsetX = 0F
                    offsetX = 0F
                    offsetTension = 0F
                    tempPoints = null
                }
            }
        }) {
            val noteWidthPx = noteWidth.value.toPx()
            val end = start + size.width / noteWidthPx
            // binary search for start point
            points.read()
            val range = valueRange.range

            // draw points
            val tmpOffsetX = offsetX
            val tmpOffsetY = offsetY
            val tmpOffsetTension = offsetTension
            val movingPoints = tempPoints
            var lastTextX = Float.NEGATIVE_INFINITY
            var lastTextY = Float.NEGATIVE_INFINITY
            if (action == EditAction.MOVE && movingPoints != null) {
                val first = movingPoints.firstOrNull()
                val isFirstSelected = first != null && selectedPoints.contains(first)
                val firstPointTime = if (first == null) 0F else (first.time + (if (isFirstSelected) tmpOffsetX else 0F)) - start
                if (first == null || firstPointTime > 0) {
                    val y = size.height * (1 - mapValue(if (first == null) valueRange.first.toFloat() else
                        first.value + (if (isFirstSelected) tmpOffsetY else 0F), valueRange))
                    drawLine(if (isFirstSelected) primaryColor else color,
                        Offset(0F, y),
                        Offset(if (first == null) size.width else firstPointTime * noteWidthPx, y), 2F
                    )
                }

                val tmpStartIndex = (movingPoints.binarySearch { it.time < start } - 1).coerceAtLeast(0)
                for (i in tmpStartIndex until points.size) {
                    val cur = movingPoints[i]
                    val isSelected = selectedPoints.contains(cur)
                    val currentTime = cur.time + (if (isSelected) tmpOffsetX else 0F)
                    if (currentTime > end) break

                    val next = movingPoints.getOrNull(i + 1)
                    val startX = (currentTime - start) * noteWidthPx
                    val isNextSelected = if (next == null) isSelected else selectedPoints.contains(next)
                    val endX = if (next == null) size.width else (next.time - start +
                            (if (isNextSelected) tmpOffsetX else 0F)) * noteWidthPx
                    val curValue = cur.value + (if (isSelected) tmpOffsetY else 0F)
                    val nextPoint = next ?: cur
                    val currentY = size.height * (1 - curValue.coerceIn(valueRange) / range)
                    if (lastTextX + 34 < startX || (lastTextY - currentY).absoluteValue > 30) {
                        lastTextX = startX
                        lastTextY = currentY
                        drawText(
                            measurer,
                            if (isDecimal) "%.2f".format(curValue) else curValue.toInt().toString(),
                            Offset(startX + 6, if (currentY + 20 > size.height) currentY - 20 else currentY + 2),
                            textStyle
                        )
                    }
                    drawEnvelope(cur.type, if (isSelected) primaryColor else color,
                        cur.tension, startX, endX, mapValue(curValue, valueRange),
                        mapValue(nextPoint.value + (if (isNextSelected) tmpOffsetY else 0F), valueRange))
                    drawCircle(if (isSelected) primaryColor else color, 4F, Offset(startX, currentY))
                }
            } else {
                startIndex = (points.binarySearch { it.time < start } - 1).coerceAtLeast(0)

                val first = points.firstOrNull()
                if (first == null || first.time > start) {
                    val y = size.height * (1 - mapValue(first?.value ?: valueRange.first.toFloat(), valueRange))
                    drawLine(if (first == null || !selectedPoints.contains(first)) color else primaryColor,
                        Offset(0F, y),
                        Offset(if (first == null) size.width else (first.time - start) * noteWidthPx, y)
                    )
                }

                val hoveredId = hoveredIndex
                for (i in startIndex until points.size) {
                    val cur = points[i]
                    if (cur.time > end) break

                    val next = points.getOrNull(i + 1)
                    val startX = (cur.time - start) * noteWidthPx
                    val endX = if (next == null) size.width else (next.time - start) * noteWidthPx
                    val isSelected = selectedPoints.contains(cur)
                    val currentY = size.height * (1 - cur.value.coerceIn(valueRange) / range)
                    if (lastTextX + 34 < startX || (lastTextY - currentY).absoluteValue > 30) {
                        lastTextX = startX
                        lastTextY = currentY
                        drawText(
                            measurer,
                            if (isDecimal) "%.2f".format(cur.value) else cur.value.toInt().toString(),
                            Offset(startX + 6, if (currentY + 20 > size.height) currentY - 20 else currentY + 2),
                            textStyle
                        )
                    }
                    drawEnvelope(cur.type, if (isSelected) primaryColor else color,
                        (cur.tension + (if (isSelected || currentAdjustingPoint == i) tmpOffsetTension else 0F)).coerceIn(-1F, 1F),
                        startX, endX, mapValue(cur.value, valueRange), mapValue((next ?: cur).value, valueRange))
                    drawCircle(if (isSelected) primaryColor else color, if (hoveredId == i) 8F else 4F, Offset(startX, currentY))
                }
            }

            // draw selection area
            if (action == EditAction.SELECT) {
                val startX = min(selectionStartX, tmpOffsetX)
                val startY = min(selectionStartY, tmpOffsetY)
                val endX = max(selectionStartX, tmpOffsetX)
                val endY = max(selectionStartY, tmpOffsetY)
                val pos = Offset(startX, startY)
                val size = Size(endX - startX, endY - startY)
                drawRect(primaryColor.copy(0.1F), pos, size)
                drawRect(primaryColor, pos, size, style = Stroke1PX)
            }
        }
    }
}
