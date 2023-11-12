@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.eimsound.daw.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitPointerSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.components.utils.x
import com.eimsound.daw.utils.fitInUnit
import com.eimsound.daw.utils.range

private val BOTTOM = ParagraphStyle(lineHeight = 16.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.FirstLineTop))

fun calcDrag(x0: Float, noteWidth: Float, editUnit: Int, range: IntRange?, onRangeChange: ((IntRange) -> Unit)?,
             onTimeChange: ((Int) -> Unit)?) {
    val x = x0.coerceAtLeast(0F)
    val newValue = (x / noteWidth).fitInUnit(editUnit)
    if (range == null) onTimeChange?.invoke(newValue)
    else if (onRangeChange != null) {
        onRangeChange(if (x - range.first * noteWidth < range.last * noteWidth - x) {
            if (newValue > range.last) range.last..newValue else newValue..range.last
        } else {
            if (newValue < range.first) newValue..range.first else range.first..newValue
        })
    }
}

val TIMELINE_HEIGHT = 40.dp

@Composable
fun Timeline(modifier: Modifier = Modifier, noteWidth: MutableState<Dp>, scrollState: ScrollState,
             range: IntRange? = null, offsetX: Dp = 0.dp, editUnit: Int = 96, barPPQ: Int = 96 * 4,
             onTimeChange: ((Int) -> Unit)? = null, onRangeChange: ((IntRange) -> Unit)? = null) {
    val tmpArr = remember { intArrayOf(editUnit, barPPQ) }
    tmpArr[0] = editUnit
    tmpArr[1] = barPPQ
    Surface(modifier.height(TIMELINE_HEIGHT).fillMaxWidth().zIndex(2F).pointerHoverIcon(PointerIcon.HorizontalResize),
        shadowElevation = 5.dp, tonalElevation = 4.dp) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val primaryColor = MaterialTheme.colorScheme.primary
        val rangeColor = primaryColor.copy(0.2F)
        val color = LocalContentColor.current
        val boldText = SpanStyle(color, MaterialTheme.typography.labelLarge.fontSize, FontWeight.Bold)
        val normalText = SpanStyle(color, MaterialTheme.typography.labelMedium.fontSize)
        val textMeasurer = rememberTextMeasurer()
        var isInRange by remember { mutableStateOf(false) }
        val obj = remember { arrayOf(range) }
        obj[0] = range

        val rangeHandleWidth by animateFloatAsState(if (isInRange) 4F else 2F)
        val onRangeChangeValue by rememberUpdatedState(onRangeChange)
        val onTimeChangeValue by rememberUpdatedState(onTimeChange)

        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                var event: PointerEvent
                do {
                    event = awaitPointerEvent(PointerEventPass.Main)
                    val width = 2 * density
                    val range0 = obj[0]
                    if (range0 != null) when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> {
                            val x = event.x + scrollState.value - offsetX.toPx()
                            val noteWidthPx = noteWidth.value.toPx()
                            val start = range0.first * noteWidthPx
                            val end = range0.last * noteWidthPx
                            isInRange = x in (start - width)..(start + width) || x in (end - width)..(end + width)
                        }

                        PointerEventType.Exit -> isInRange = false
                    }
                    if (event.buttons.isPrimaryPressed || (range0 != null && event.buttons.isSecondaryPressed)) break
                } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
                val down = event.changes[0]
                var drag: PointerInputChange?
                do {
                    drag = awaitPointerSlopOrCancellation(
                        down.id,
                        down.type,
                        triggerOnMainAxisSlop = false
                    )
                    { change, _ -> change.consume() }
                } while (drag != null && !drag.isConsumed)
                if (obj[0] != null && event.buttons.isSecondaryPressed) isInRange = true
                calcDrag(
                    down.position.x + scrollState.value - offsetX.toPx(), noteWidth.value.toPx(),
                    tmpArr[0], if (isInRange) obj[0] else null, onRangeChangeValue, onTimeChange
                )
                if (drag != null) {
                    !drag(drag.id) {
                        calcDrag(
                            it.position.x + scrollState.value - offsetX.toPx(), noteWidth.value.toPx(),
                            tmpArr[0], if (isInRange) obj[0] else null, onRangeChangeValue, onTimeChange
                        )
                        it.consume()
                    }
                }
                isInRange = false
            }
            detectDragGestures { change, _ ->
                change.consume()
                onTimeChangeValue?.invoke(((change.position.x + scrollState.value - offsetX.value) / noteWidth.value.toPx()).toInt())
            }
        }) {
            val offsetXValue = offsetX.toPx()
            val noteWidthPx = noteWidth.value.toPx()
            val barWidth = noteWidthPx * tmpArr[1]
            val startBar = (scrollState.value / barWidth).toInt()
            val endBar = ((scrollState.value + size.width - offsetXValue) / barWidth).toInt()
            scrollState.maxValue // mark as read state

            val shouldSkipBars = noteWidth.value.value < 0.065F

            for (i in startBar..endBar) {
                val x = i * barWidth - scrollState.value
                if (x < 0) continue
                drawLine(outlineColor, Offset(offsetXValue + x, 26f * density),
                    Offset(offsetXValue + x, size.height), density)
                if (shouldSkipBars && i % 4 != 0) continue
                val result = textMeasurer.measure(AnnotatedString("${i + 1}", if (i % 4 == 0) boldText else normalText, BOTTOM),
                    maxLines = 1)
                drawText(result, color, topLeft = Offset(offsetXValue + x +
                        (if (i == 0 && offsetXValue == 0F) 4 else -result.size.width / 2), 10f))
            }

            if (range != null) {
                val start = range.first * noteWidthPx - scrollState.value + offsetXValue
                val end = range.last * noteWidthPx - scrollState.value + offsetXValue
                // draw a rect with rangeColor in startPPQ to endPPQ
                drawRect(rangeColor, Offset(start, 0F),
                    Size(range.range * noteWidthPx, size.height))

                // draw two 2px lines in above rect start and end with primaryColor
                val width = rangeHandleWidth * density
                drawLine(primaryColor, Offset(start, 0F), Offset(start, size.height), width)
                drawLine(primaryColor, Offset(end, 0F), Offset(end, size.height), width)
            }
        }
    }
}

@Composable
fun PlayHead(
    noteWidth: MutableState<Dp>, scrollState: ScrollState, position: Float, width: Dp? = null,
    offsetX: Dp = 0.dp, color: Color = MaterialTheme.colorScheme.onBackground
) {
    var playHeadPosition = noteWidth.value * position - with (LocalDensity.current) { scrollState.value.toDp() }
    if (playHeadPosition.value < 0 || (width != null && playHeadPosition > width)) return
    playHeadPosition += offsetX
    Box(Modifier.fillMaxHeight().width(1.dp).offset(playHeadPosition).background(color))
    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)
        .offset(playHeadPosition - 8.dp, (-6).dp).rotate(90F), tint = color)
}
