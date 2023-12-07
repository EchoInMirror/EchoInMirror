package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.EditorTool
import com.eimsound.daw.commons.json.JsonIgnoreDefaults
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.commons.SerializableEditor
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.calculateContrastRatio
import com.eimsound.daw.utils.*
import com.eimsound.dsp.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.collections.ArrayDeque
import kotlin.math.*

@Suppress("DuplicatedCode")
fun EnvelopeType.toPath(
    height: Float, tension: Float, x0: Float, x1: Float,
    value0: Float, value1: Float
) = Path().apply {
    if (value0 == value1) {
        val y0True = (1 - value0) * height
        moveTo(x0, y0True)
        lineTo(x1, y0True)
        return@apply
    }
    when (this@toPath) {
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
            moveTo(x0, (1 - value0) * height)
            cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X,
                (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
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
            moveTo(x0, (1 - value0) * height)
            cubicTo(controlPoint1X, (1 - controlPoint1Y) * height, controlPoint2X,
                (1 - controlPoint2Y) * height, x1, (1 - value1) * height)
        }
        EnvelopeType.SQUARE -> {
            val y0True = (1 - value0) * height
            moveTo(x0, y0True)
            lineTo(x1 - 0.5F, y0True)
            lineTo(x1, (1 - value1) * height)
        }
    }
}

private fun Density.getSelectedPoint(
    position: Offset, points: EnvelopePointList, start: Float,
    valueRange: FloatRange, noteWidth: MutableState<Dp>, height: Int
): Pair<Int, Int> {
    if (points.isEmpty()) return -1 to -1
    val noteWidthPx = noteWidth.value.toPx()
    val targetX = start + position.x / noteWidthPx
    val pointIndex = points.lowerBound { it.time <= targetX }
    val point = points[pointIndex]
    fun checkIsSelectedPoint(point: EnvelopePoint?) = point != null && (point.time - targetX).absoluteValue < 8F / noteWidthPx &&
            (height * (1 - point.value.coerceIn(valueRange) / valueRange.range) - position.y).absoluteValue < 8 * density
    return (if (point.time < targetX) pointIndex else pointIndex - 1) to if (checkIsSelectedPoint(point)) pointIndex else
        if (checkIsSelectedPoint(points.getOrNull(pointIndex - 1))) pointIndex - 1 else -1
}

interface EnvelopeEditorEventHandler {
    fun onAddPoints(editor: EnvelopeEditor, points: BaseEnvelopePointList)
    fun onPastePoints(editor: EnvelopeEditor, points: BaseEnvelopePointList): BaseEnvelopePointList
    fun onRemovePoints(editor: EnvelopeEditor, points: BaseEnvelopePointList)
    fun onMovePoints(editor: EnvelopeEditor, points: BaseEnvelopePointList, offsetTime: Int, offsetValue: Float)
    fun onTensionChanged(editor: EnvelopeEditor, points: BaseEnvelopePointList, tension: Float)
    fun onTypeChanged(editor: EnvelopeEditor, points: BaseEnvelopePointList, type: EnvelopeType)
    fun onMultiplePointsChanged(editor: EnvelopeEditor, points: BaseEnvelopePointList)
}

private fun floatToFixed(value: Float): String {
    val n = (value * 100).roundToInt()
    val f = n / 100
    val str = f.toString()
    val end = (n % 100).absoluteValue
    if (end == 0) return str
    if (end < 0) return "$str.0$end"
    val g = end % 10
    return "$str.${if (g == 0) end / 10 else end}"
}

class EnvelopeEditor(
    val points: EnvelopePointList, val valueRange: FloatRange,
    private val defaultValue: Float = valueRange.start, private val isFloat: Boolean = false,
    private val horizontalScrollState: ScrollState? = null,
    private val eventHandler: EnvelopeEditorEventHandler? = null,
    private val getEditorTool: () -> EditorTool = { EchoInMirror.editorTool },
): SerializableEditor, MultiSelectableEditor {
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
    private var movingPoints: MutableBaseEnvelopePointList? = null
    private var currentAdjustingPoint = -1
    private var positionInRoot = Offset.Zero
    private var tempAddPoints = ArrayDeque<EnvelopePoint>()
    private var modification by mutableStateOf<Byte>(0)

    private val isPencil get() = getEditorTool() == EditorTool.PENCIL

    companion object {
        var copiedPoints: BaseEnvelopePointList? = null
        var defaultTension = 0F
    }

    private fun copyAsObject(): BaseEnvelopePointList {
        val startTime = selectedPoints.minOf { it.time }
        return selectedPoints.map { it.copy(it.time - startTime, mapValue(it.value, valueRange)) }
    }

    override fun copy() {
        if (selectedPoints.isEmpty()) return
        copiedPoints = copyAsObject()
    }
    override fun paste() {
        val tmp = copiedPoints ?: return
        if (tmp.isEmpty()) return
        val result = eventHandler?.onPastePoints(this, tmp) ?: return
        selectedPoints.clear()
        selectedPoints.addAll(result)
    }
    override fun copyAsString() = if (selectedPoints.isEmpty()) "" else JsonIgnoreDefaults.encodeToString(
        SerializableEnvelopePointList(EchoInMirror.currentPosition.ppq, copyAsObject())
    )

    override fun pasteFromString(value: String) {
        try {
            val paste = JsonIgnoreDefaults.decodeFromString<SerializableEnvelopePointList>(value)
            val factor = EchoInMirror.currentPosition.ppq.toDouble() / paste.ppq
            paste.points.fastForEach { it.time = (it.time * factor).roundToInt() }
            val result = eventHandler?.onPastePoints(this, paste.points) ?: return
            selectedPoints.clear()
            selectedPoints.addAll(result)
        } catch (ignored: Throwable) { }
    }

    override fun delete() { eventHandler?.onRemovePoints(this, selectedPoints.toList()) }
    override fun selectAll() { selectedPoints.addAll(points) }

    private suspend fun AwaitPointerEventScope.handlePointerEvent(
        noteWidth: MutableState<Dp>, floatingLayerProvider: FloatingLayerProvider, scope: CoroutineScope
    ) {
        var event: PointerEvent
        do {
            event = awaitPointerEvent(PointerEventPass.Main)
            when (event.type) {
                PointerEventType.Move -> {
                    // find the hovered point
                    val (_, tmpId) = getSelectedPoint(
                        event.changes[0].position,
                        points,
                        startValue,
                        valueRange,
                        noteWidth,
                        size.height
                    )
                    if (tmpId != hoveredIndex) hoveredIndex = tmpId
                    continue
                }

                PointerEventType.Press -> {
                    val x = event.changes[0].position.x
                    val y = event.changes[0].position.y
                    if (event.keyboardModifiers.isCrossPlatformCtrlPressed || event.buttons.isForwardPressed) {
                        selectedPoints.clear()
                        selectionStartX = x
                        selectionStartY = y
                        offsetX = x
                        offsetY = y
                        action = EditAction.SELECT
                        break
                    }
                    // find the selected point
                    val (pointId, tmpId) = getSelectedPoint(
                        event.changes[0].position, points,
                        startValue, valueRange, noteWidth, size.height
                    )
                    val isPointExists = pointId >= 0 && pointId < points.size - 1
                    if (tmpId == -1) {
                        if (event.buttons.isSecondaryPressed) {
                            if (isPointExists && selectedPoints.isEmpty()) selectedPoints.add(points[pointId])
                            floatingLayerProvider.openMenu(
                                event.changes[0].position,
                                if (isPointExists) points[pointId] else null,
                            )
                            continue
                        }
                        if (isPencil) {
                            selectedPoints.clear()
                            action = EditAction.BRUSH
                            break
                        } else if (isPointExists) {
                            if (!selectedPoints.contains(points[pointId])) selectedPoints.clear()
                            currentAdjustingPoint = pointId
                            action = EditAction.RESIZE
                            break
                        }
                    } else if (isPencil) {
                        selectedPoints.clear()
                        action = EditAction.BRUSH
                        break
                    } else {
                        if (!selectedPoints.contains(points[tmpId])) {
                            selectedPoints.clear()
                            selectedPoints.add(points[tmpId])
                        }
                        hoveredIndex = tmpId
                        if (event.buttons.isSecondaryPressed) {
                            floatingLayerProvider.openMenu(event.changes[0].position, points[tmpId])
                            continue
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
            return
        }
        var selectedPointsLeft = Int.MAX_VALUE
//                    var selectedPointsRight = Int.MIN_VALUE
        var selectedPointsTop = valueRange.start
        var selectedPointsBottom = valueRange.endInclusive
        when (action) {
            EditAction.MOVE -> {
                selectedPoints.forEach {
                    if (it.time < selectedPointsLeft) selectedPointsLeft = it.time
                    //                            if (it.time > selectedPointsRight) selectedPointsRight = it.time
                    if (it.value > selectedPointsTop) selectedPointsTop = it.value
                    if (it.value < selectedPointsBottom) selectedPointsBottom = it.value
                }
                movingPoints = points.toMutableList()
            }

            EditAction.BRUSH -> tempAddPoints.addAll(points.copy())

            else -> { }
        }

        var addTimes = 0
        drag(drag.id) {
            var y = it.position.y
            if (action == EditAction.RESIZE) {
                y = -((y - downY) / size.height).coerceIn(-2F, 2F)
                if (offsetTension != y) offsetTension = y
            } else {
                var x = it.position.x
                when (action) {
                    EditAction.MOVE -> {
                        y = -((y - downY) / size.height).coerceIn(-1F, 1F) * valueRange.range
                        if (!isFloat) y = round(y)
                        x = (((x + (horizontalScrollState?.value?.toFloat()
                            ?: 0F)).coerceAtLeast(0F) - downX) /
                                noteWidth.value.toPx()).fitInUnit(editUnitValue).toFloat()
                        if (selectedPointsLeft + x + clipStartTimeValue < 0) x =
                            -(selectedPointsLeft.toFloat() + clipStartTimeValue)
                        if (selectedPointsTop + y > valueRange.endInclusive) y =
                            valueRange.endInclusive - selectedPointsTop
                        if (selectedPointsBottom + y < valueRange.start) y =
                            valueRange.start - selectedPointsBottom
                        if (x != offsetX) movingPoints?.sortBy { p -> p.time + (if (selectedPoints.contains(p)) x else 0F) }
                    }

                    EditAction.BRUSH -> {
                        val cur = (startValue + x / noteWidth.value.toPx()).fitInUnit(editUnitValue)
                        val curIndex = tempAddPoints.lowerBound { p -> p.time < cur }
                        val curObj = tempAddPoints.getOrNull(curIndex)
                        val value = (1 - y / size.height) * valueRange.range + valueRange.start
                        if (curObj == null || curObj.time != cur) {
                            tempAddPoints.add(curIndex, EnvelopePoint(cur, value.coerceIn(valueRange), defaultTension))
                        } else {
                            curObj.value = value.coerceIn(valueRange)
                        }
                        when (addTimes) {
                            0, 1 -> addTimes++
                            2 -> tempAddPoints.sort()
                        }
                        modification++
                    }

                    else -> { }
                }
                if (x != offsetX) offsetX = x
                if (y != offsetY) offsetY = y
                if (horizontalScrollState != null) {
                    if (it.position.x < 10) scope.launch { horizontalScrollState.scrollBy(-3F) }
                    else if (it.position.x > size.width - 10) scope.launch {
                        horizontalScrollState.scrollBy(3F)
                    }
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
                    it.time >= startValue + startX && it.time <= startValue + endX && y >= startY && y <= endY
                })
            }

            EditAction.MOVE ->
                eventHandler?.onMovePoints(
                    this@EnvelopeEditor,
                    selectedPoints.toList(),
                    offsetX.toInt(),
                    offsetY
                )

            EditAction.RESIZE -> {
                val point = points[currentAdjustingPoint]
                defaultTension = (point.tension + offsetTension).coerceIn(-1F, 1F)
                eventHandler?.onTensionChanged(
                    this@EnvelopeEditor, if (selectedPoints.isEmpty())
                        listOf(point) else selectedPoints.toList(), offsetTension
                )
            }

            EditAction.BRUSH -> {
                eventHandler?.onMultiplePointsChanged(this@EnvelopeEditor, tempAddPoints)
                action = EditAction.NONE
                tempAddPoints.clear()
            }

            else -> {}
        }

        action = EditAction.NONE
        selectionStartX = 0F
        selectionStartY = 0F
        offsetX = 0F
        offsetX = 0F
        offsetTension = 0F
        movingPoints = null
    }

    @Suppress("DuplicatedCode")
    @Composable
    fun Editor(
        start: Float, color: Color, noteWidth: MutableState<Dp>, showThumb: Boolean = true, editUnit: Int = 24,
        clipStartTime: Int = 0, stroke: Float = 2F,
        backgroundColor: Color? = null, drawGradient: Boolean = true
    ) {
        val scope = rememberCoroutineScope()
        val measurer = rememberTextMeasurer(50)
        val floatingLayerProvider = LocalFloatingLayerProvider.current
        var primaryColor = MaterialTheme.colorScheme.primary
        if (backgroundColor != null && backgroundColor.calculateContrastRatio(primaryColor) < 0.3F)
            primaryColor = MaterialTheme.colorScheme.inversePrimary
        val textStyle = MaterialTheme.typography.labelMedium
        val fillColor = verticalGradient(
            0F to color.copy(0.4F),
            1.4F to Color.Transparent
        )
        val primaryFillColor = verticalGradient(
            0F to primaryColor.copy(0.4F),
            1F to Color.Transparent
        )
        startValue = start
        clipStartTimeValue = clipStartTime
        editUnitValue = editUnit

//        if (hoveredIndex == -1 && action == EditAction.RESIZE) modifier = modifier.pointerHoverIcon(PointerIconDefaults.VerticalResize)

        Canvas(Modifier.fillMaxSize().graphicsLayer { }.onGloballyPositioned { positionInRoot = it.positionInRoot() }.run {
            if (eventHandler == null) this else pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (
                        isPencil ||
                        getSelectedPoint(it, points, startValue, valueRange, noteWidth, size.height).second != -1
                    ) return@detectTapGestures
                    var newValue = (1 - it.y / size.height) * valueRange.range + valueRange.start
                    if (!isFloat) newValue = round(newValue)
                    val targetX = startValue + it.x / noteWidth.value.toPx()
                    val newPoint = EnvelopePoint(targetX.fitInUnit(editUnitValue), newValue.coerceIn(valueRange), defaultTension)
                    eventHandler.onAddPoints(this@EnvelopeEditor, listOf(newPoint))
                })
            }.run {
                if (isPencil) pointerHoverIcon(EditorTool.PENCIL.pointerIcon) else this
            }.pointerInput(this@EnvelopeEditor) {
                awaitEachGesture { handlePointerEvent(noteWidth, floatingLayerProvider, scope) }
            }
        }) {
            val strokeWidth = stroke * density
            val thumbSize = 2 * strokeWidth.coerceAtLeast(density)
            val strokeStyle = Stroke(strokeWidth)
            val noteWidthPx = noteWidth.value.toPx()
            val end = start + size.width / noteWidthPx
            // binary search for start point
            points.read()
            val range = valueRange.range
            val hoveredId = hoveredIndex

            // draw points
            val tmpOffsetX = offsetX
            val tmpOffsetY = offsetY
            val tmpOffsetTension = offsetTension
            val movingPoints = this@EnvelopeEditor.movingPoints
            var lastTextX = Float.NEGATIVE_INFINITY
            var lastTextY = Float.NEGATIVE_INFINITY

            val maxTextSize = Constraints(maxWidth = (30 * density).toInt(), maxHeight = (20 * density).toInt())

            if (action == EditAction.MOVE && movingPoints != null) {
                val first = movingPoints.firstOrNull()
                val isFirstSelected = first != null && selectedPoints.contains(first)
                val firstPointTime =
                    if (first == null) 0F else (first.time + (if (isFirstSelected) tmpOffsetX else 0F)) - start
                if (first == null || firstPointTime > 0) {
                    val x = if (first == null) size.width else firstPointTime * noteWidthPx
                    val y = size.height * (1 - mapValue(
                        if (first == null) defaultValue else
                            first.value + (if (isFirstSelected) tmpOffsetY else 0F), valueRange
                    ))
                    val topLeft = Offset(0F, y)
                    if (drawGradient)
                        drawRect(if (isFirstSelected) primaryFillColor else fillColor, topLeft, Size(x, size.height - y))
                    drawLine(if (isFirstSelected) primaryColor else color, topLeft, Offset(x, y), strokeWidth)
                }

                val tmpStartIndex = (movingPoints.lowerBound { it.time < start } - 1).coerceAtLeast(0)
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
                    val curColor = if (isSelected) primaryColor else color
                    if (showThumb && (lastTextX + 34 * density < startX || (lastTextY - currentY).absoluteValue > 30 * density)) {
                        lastTextX = startX
                        lastTextY = currentY
                        val result = measurer.measure(
                            AnnotatedString(if (isFloat) floatToFixed(curValue) else curValue.toInt().toString()),
                            textStyle,
                            maxLines = 1,
                            constraints = maxTextSize
                        )
                        drawText(
                            result,
                            curColor,
                            Offset(
                                startX + 6 * density,
                                if (currentY + result.size.height > size.height) currentY - result.size.height else currentY + density
                            )
                        )
                    }
                    val path = cur.type.toPath(
                        size.height,
                        cur.tension, startX, endX, mapValue(curValue, valueRange),
                        mapValue(nextPoint.value + (if (isNextSelected) tmpOffsetY else 0F), valueRange)
                    )
                    drawPath(path, curColor, style = strokeStyle)
                    if (drawGradient) drawPath(Path().apply {
                        addPath(path)
                        lineTo(endX, size.height)
                        lineTo(startX, size.height)
                        close()
                    }, if (isSelected) primaryFillColor else fillColor)
                    if (showThumb) drawCircle(curColor, thumbSize * (if (hoveredId == i) 2 else 1), Offset(startX, currentY))
                }
            } else {
                modification
                val points = if (action == EditAction.BRUSH && tempAddPoints.isNotEmpty()) tempAddPoints else points
                startIndex = (points.lowerBound { it.time < start } - 1).coerceAtLeast(0)

                val first = points.firstOrNull()
                if (first == null || first.time > start) {
                    val x = if (first == null) size.width else (first.time - start) * noteWidthPx
                    val y = size.height * (1 - mapValue(first?.value ?: defaultValue, valueRange))
                    val isFirstSelected = first != null && selectedPoints.contains(first)
                    val topLeft = Offset(0F, y)
                    if (drawGradient)
                        drawRect(if (isFirstSelected) primaryFillColor else fillColor, topLeft, Size(x, size.height - y))
                    drawLine(if (isFirstSelected) primaryColor else color, topLeft, Offset(x, y), strokeWidth)
                }

                for (i in startIndex until points.size) {
                    val cur = points[i]
                    if (cur.time > end) break

                    val next = points.getOrNull(i + 1)
                    val startX = (cur.time - start) * noteWidthPx
                    val endX = if (next == null) size.width else (next.time - start) * noteWidthPx
                    val isSelected = selectedPoints.contains(cur)
                    val currentY = size.height * (1 - cur.value.coerceIn(valueRange) / range)
                    val curColor = if (isSelected) primaryColor else color
                    if (showThumb && (lastTextX + 34 * density < startX || (lastTextY - currentY).absoluteValue > 30 * density)) {
                        lastTextX = startX
                        lastTextY = currentY
                        val result = measurer.measure(
                            AnnotatedString(if (isFloat) floatToFixed(cur.value) else cur.value.toInt().toString()),
                            textStyle,
                            maxLines = 1,
                            constraints = maxTextSize
                        )
                        drawText(
                            result,
                            curColor,
                            Offset(
                                startX + 6 * density,
                                if (currentY + result.size.height > size.height) currentY - result.size.height else currentY + density
                            )
                        )
                    }
                    val path = cur.type.toPath(
                        size.height,
                        (cur.tension + (if (isSelected || currentAdjustingPoint == i) tmpOffsetTension else 0F)).coerceIn(
                            -1F,
                            1F
                        ),
                        startX,
                        endX,
                        mapValue(cur.value, valueRange),
                        mapValue((next ?: cur).value, valueRange)
                    )
                    drawPath(path, curColor, style = strokeStyle)
                    if (drawGradient) drawPath(Path().apply {
                        addPath(path)
                        lineTo(endX, size.height)
                        lineTo(startX, size.height)
                        close()
                    }, if (isSelected) primaryFillColor else fillColor)
                    if (showThumb) drawCircle(curColor, thumbSize * (if (hoveredId == i) 2 else 1), Offset(startX, currentY))
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
                drawRect(primaryColor, pos, size, style = Stroke(density))
            }
        }
    }

    private fun FloatingLayerProvider.openMenu(offset: Offset, point: EnvelopePoint?) {
        openEditorMenu(offset + positionInRoot, this@EnvelopeEditor) { close ->
            points.read()
            val currentPoint = (if (selectedPoints.contains(point)) point else selectedPoints.firstOrNull()) ?: return@openEditorMenu
            MenuItem(padding = PaddingValues(12.dp, 12.dp, 12.dp, 0.dp)) {
                Text("值:")
                Filled()
                CustomTextField(
                    if (isFloat) floatToFixed(currentPoint.value) else currentPoint.value.toInt().toString(),
                    {
                        val value = it.toFloatOrNull()?.coerceIn(valueRange) ?: return@CustomTextField
                        eventHandler?.onMovePoints(this@EnvelopeEditor, selectedPoints.toList(), 0,
                            (if (isFloat) value else round(value)) - currentPoint.value)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            MenuItem(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                EnvelopeType.entries.forEach {
                    EnvelopeTypeToggleButton(it, currentPoint.type == it) { checked ->
                        close()
                        if (checked) eventHandler?.onTypeChanged(this@EnvelopeEditor, selectedPoints.toList(), it)
                    }
                }
            }
            Divider()
        }
    }
}

@Composable
private fun EnvelopeTypeToggleButton(type: EnvelopeType, selected: Boolean, onClick: (Boolean) -> Unit) {
    FilledIconToggleButton(selected, Modifier.size(30.dp), onClick) {
        val color = LocalContentColor.current
        Canvas(Modifier.size(14.dp)) {
            drawPath(
                type.toPath(size.height, 0.5F, 0F, size.width, 0F, 1F),
                color, style = Stroke(density * 2)
            )
        }
    }
}
