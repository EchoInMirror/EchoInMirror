package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.data.getEditUnit
import cn.apisium.eim.utils.HorizontalResize
import cn.apisium.eim.utils.fitInUnit
import cn.apisium.eim.utils.x

private val BOTTOM = ParagraphStyle(lineHeight = 16.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.FirstLineTop))

fun calcDrag(x0: Float, noteWidth: Float, range: IntRange?, onRangeChange: ((IntRange) -> Unit)?) {
    val x = x0.coerceAtLeast(0F)
    if (range == null) EchoInMirror.currentPosition.setCurrentTime((x / noteWidth).toInt())
    else if (onRangeChange != null) {
        val newValue = (x / noteWidth).fitInUnit(getEditUnit())
        onRangeChange(if (x - range.first * noteWidth < range.last * noteWidth - x) {
            if (newValue > range.last) range.last..newValue else newValue..range.last
        } else {
            if (newValue < range.first) newValue..range.first else range.first..newValue
        })
    }
}

val TIMELINE_HEIGHT = 40.dp

@Suppress("DuplicatedCode")
@OptIn(ExperimentalTextApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Timeline(modifier: Modifier = Modifier, noteWidth: MutableState<Dp>, scrollState: ScrollState,
             range: IntRange? = null, offsetX: Dp = 0.dp, onRangeChange: ((IntRange) -> Unit)? = null) {
    Surface(modifier.height(TIMELINE_HEIGHT).fillMaxWidth().zIndex(2F).pointerHoverIcon(PointerIconDefaults.HorizontalResize),
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
        remember(range) { obj[0] = range }

        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    val noteWidthPx = noteWidth.value.toPx()
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent(PointerEventPass.Main)
                        val range0 = obj[0]
                        if (range0 != null) when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> {
                                val x = event.x + scrollState.value - offsetX.toPx()
                                val start = range0.first * noteWidthPx
                                val end = range0.last * noteWidthPx
                                isInRange = x in (start - 2)..(start + 2) || x in (end - 2)..(end + 2)
                            }
                            PointerEventType.Exit -> isInRange = false
                        }
                        if (event.buttons.isPrimaryPressed || (range0 != null && event.buttons.isSecondaryPressed)) break
                    } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
                    val down = event.changes[0]
                    var drag: PointerInputChange?
                    do {
                        @Suppress("INVISIBLE_MEMBER")
                        drag = awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false)
                            { change, _ -> change.consume() }
                    } while (drag != null && !drag.isConsumed)
                    if (obj[0] != null && event.buttons.isSecondaryPressed) isInRange = true
                    calcDrag(down.position.x + scrollState.value - offsetX.toPx(), noteWidthPx,
                        if (isInRange) obj[0] else null, onRangeChange)
                    if (drag != null) {
                        !drag(drag.id) {
                            calcDrag(it.position.x + scrollState.value - offsetX.toPx(), noteWidthPx,
                                if (isInRange) range else null, onRangeChange)
                            it.consume()
                        }
                    }
                    isInRange = false
                }
            }
            detectDragGestures { change, _ ->
                change.consume()
                EchoInMirror.currentPosition.setCurrentTime(((change.position.x + scrollState.value - offsetX.value) / noteWidth.value.toPx()).toInt())
            }
        }) {
            val offsetXValue = offsetX.toPx()
            val noteWidthPx = noteWidth.value.toPx()
            val barWidth = noteWidthPx * EchoInMirror.currentPosition.ppq * EchoInMirror.currentPosition.timeSigNumerator
            val startBar = (scrollState.value / barWidth).toInt()
            val endBar = ((scrollState.value + size.width - offsetXValue) / barWidth).toInt()
            scrollState.maxValue // mark as read state

            for (i in startBar..endBar) {
                val x = i * barWidth - scrollState.value
                if (x < 0) continue
                drawLine(outlineColor, Offset(offsetXValue + x, 26f),
                    Offset(offsetXValue + x, size.height), 1F)
                val result = textMeasurer.measure(AnnotatedString("${i + 1}", if (i % 4 == 0) boldText else normalText, BOTTOM),
                    maxLines = 1)
                drawText(result, topLeft = Offset(offsetXValue + x +
                        (if (i == 0 && offsetXValue == 0F) 4 else -result.size.width / 2), 10f))
            }

            if (range != null) {
                val start = range.first * noteWidthPx - scrollState.value + offsetXValue
                val end = range.last * noteWidthPx - scrollState.value + offsetXValue
                // draw a rect with rangeColor in startPPQ to endPPQ
                drawRect(rangeColor, Offset(start, 0F),
                    Size((range.last - range.first) * noteWidthPx, size.height))

                // draw two 2px lines in above rect start and end with primaryColor
                val width = if (isInRange) 4F else 2F
                drawLine(primaryColor, Offset(start, 0F), Offset(start, size.height), width)
                drawLine(primaryColor, Offset(end, 0F), Offset(end, size.height), width)
            }
        }
    }
}

@Composable
fun PlayHead(noteWidth: MutableState<Dp>, scrollState: ScrollState, width: Dp? = null, offsetX: Dp = 0.dp,
             color: Color = MaterialTheme.colorScheme.onBackground) {
    val currentPosition = EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.ppq
    var playHeadPosition = noteWidth.value * currentPosition.toFloat() - scrollState.value.dp
    if (playHeadPosition.value < 0 || (width != null && playHeadPosition > width)) return
    playHeadPosition += offsetX
    Box(Modifier.fillMaxHeight().width(1.dp).offset(playHeadPosition).background(color))
    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)
        .offset(playHeadPosition - 8.dp, (-6).dp).rotate(90F), tint = color)
}
