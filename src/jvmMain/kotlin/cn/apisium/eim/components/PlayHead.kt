package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror

private val MAX_TEXT_SIZE = IntSize(20, 14)
private val BOTTOM = ParagraphStyle(lineHeight = 16.sp, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.FirstLineTop))

@OptIn(ExperimentalTextApi::class)
@Composable
fun Timeline(modifier: Modifier = Modifier, noteWidth: Dp, scrollState: ScrollState, offsetX: Dp = 0.dp) {
    Surface(modifier.height(22.dp).fillMaxWidth().zIndex(2F), shadowElevation = 5.dp, tonalElevation = 4.dp) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val textMeasure = rememberTextMeasurer()
        val boldText = SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.labelLarge.fontSize)
        val normalText = SpanStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize)
        val color = LocalContentColor.current
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                change.consume()
                EchoInMirror.currentPosition.setCurrentTime(((change.position.x + scrollState.value - offsetX.value) / noteWidth.value).toInt())
            }
        }.pointerInput(Unit) {
            detectTapGestures {
                EchoInMirror.currentPosition.setCurrentTime(((it.x + scrollState.value - offsetX.value) / noteWidth.value).toInt())
            }
        }) {
            val offsetXValue = offsetX.toPx()
            val barWidth = noteWidth.toPx() * EchoInMirror.currentPosition.ppq * EchoInMirror.currentPosition.timeSigNumerator
            val startBar = (scrollState.value / barWidth).toInt()
            val endBar = ((scrollState.value + size.width - offsetXValue) / barWidth).toInt()
            for (i in startBar..endBar) {
                val x = i * barWidth - scrollState.value
                if (x < 0) continue
                drawLine(
                    color = outlineColor,
                    start = Offset(offsetXValue + x, 0f),
                    end = Offset(offsetXValue + x, size.height),
                    strokeWidth = 1F
                )
                drawText(textMeasure, AnnotatedString("${i + 1}", if (i % 4 == 0) boldText else normalText, BOTTOM),
                    Offset(offsetXValue + x + 4, 4f), TextStyle(color), maxSize = MAX_TEXT_SIZE)
            }
        }
    }
}

@Composable
fun PlayHead(noteWidth: Dp, scrollState: ScrollState, width: Dp? = null, offsetX: Dp = 0.dp,
             isCursorOnBottom: Boolean = false, color: Color = MaterialTheme.colorScheme.onBackground) {
    val currentPosition = EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.ppq
    var playHeadPosition = noteWidth * currentPosition.toFloat() - scrollState.value.dp
    if (playHeadPosition.value < 0 || (width != null && playHeadPosition > width)) return
    playHeadPosition += offsetX
    Box(Modifier.fillMaxHeight().width(1.dp).offset(playHeadPosition).background(color))
    if (isCursorOnBottom) {
        Box(Modifier.fillMaxHeight().offset(playHeadPosition - 8.dp, 6.dp), contentAlignment = Alignment.BottomStart) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)
                .rotate(-90F), tint = color)
        }
    } else Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)
        .offset(playHeadPosition - 8.dp, (-6).dp).rotate(90F), tint = color)
}
