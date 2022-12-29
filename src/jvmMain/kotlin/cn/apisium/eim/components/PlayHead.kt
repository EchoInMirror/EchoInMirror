package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import cn.apisium.eim.utils.x
import org.jetbrains.skiko.Cursor

private val BOTTOM = ParagraphStyle(lineHeight = 16.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.FirstLineTop))

fun calcDrag(isInRange: Boolean, x0: Float, noteWidth: Float) {
    val x = x0.coerceAtLeast(0F)
    if (isInRange) {
        val range = EchoInMirror.currentPosition.projectRange
        EchoInMirror.currentPosition.projectRange = if (x - range.first * noteWidth < range.last * noteWidth - x) {
            val newStart = (x / noteWidth).toInt()
            if (newStart > range.last) range.last..newStart else newStart..range.last
        } else {
            val newEnd = (x / noteWidth).toInt()
            if (newEnd < range.first) newEnd..range.first else range.first..newEnd
        }
    } else EchoInMirror.currentPosition.setCurrentTime((x / noteWidth).toInt())
}

val TIMELINE_HEIGHT = 40.dp

@Suppress("DuplicatedCode")
@OptIn(ExperimentalTextApi::class)
@Composable
fun Timeline(modifier: Modifier = Modifier, noteWidth: MutableState<Dp>, scrollState: ScrollState, drawRange: Boolean = false, offsetX: Dp = 0.dp) {
    Surface(modifier.height(TIMELINE_HEIGHT).fillMaxWidth().zIndex(2F).pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))),
        shadowElevation = 5.dp, tonalElevation = 4.dp) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val primaryColor = MaterialTheme.colorScheme.primary
        val rangeColor = primaryColor.copy(0.2F)
        val color = LocalContentColor.current
        val boldText = SpanStyle(color, MaterialTheme.typography.labelLarge.fontSize, FontWeight.Bold)
        val normalText = SpanStyle(color, MaterialTheme.typography.labelMedium.fontSize)
        val textMeasurer = rememberTextMeasurer()
        var isInRange by remember { mutableStateOf(false) }

        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    val noteWidthPx = noteWidth.value.toPx()
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> {
                                val x = event.x + scrollState.value
                                val range = EchoInMirror.currentPosition.projectRange
                                val start = range.first * noteWidthPx
                                val end = range.last * noteWidthPx
                                isInRange = x in (start - 2)..(start + 2) || x in (end - 2)..(end + 2)
                            }
                            PointerEventType.Exit -> isInRange = false
                        }
                        if (event.buttons.isPrimaryPressed || event.buttons.isSecondaryPressed) break
                    } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
                    val down = event.changes[0]
                    var drag: PointerInputChange?
                    do {
                        @Suppress("INVISIBLE_MEMBER")
                        drag = awaitPointerSlopOrCancellation(down.id, down.type, triggerOnMainAxisSlop = false)
                            { change, _ -> change.consume() }
                    } while (drag != null && !drag.isConsumed)
                    if (event.buttons.isSecondaryPressed) isInRange = true
                    calcDrag(isInRange, down.position.x + scrollState.value - offsetX.toPx(), noteWidthPx)
                    if (drag != null) {
                        !drag(drag.id) {
                            calcDrag(isInRange, it.position.x + scrollState.value - offsetX.toPx(), noteWidthPx)
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

            if (drawRange) {
                val range = EchoInMirror.currentPosition.projectRange
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
