package cn.apisium.eim.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
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
    Surface(modifier.height(18.dp).fillMaxWidth().zIndex(2F), shadowElevation = 5.dp) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val textMeasure = rememberTextMeasurer()
        val boldText = SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.labelLarge.fontSize)
        val normalText = SpanStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize)
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                change.consume()
                EchoInMirror.currentPosition.setCurrentTime(((change.position.x + scrollState.value - offsetX.value) / noteWidth.value * 1.5).toLong())
            }
        }.pointerInput(Unit) {
            detectTapGestures {
                EchoInMirror.currentPosition.setCurrentTime(((it.x + scrollState.value - offsetX.value) / noteWidth.value * 1.5).toLong())
            }
        }) {
            val offsetXValue = offsetX.toPx()
            val barWidth = noteWidth.toPx() * 16 * EchoInMirror.currentPosition.timeSigDenominator * EchoInMirror.currentPosition.timeSigNumerator
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
                    Offset(offsetXValue + x + 4, 0f), maxSize = MAX_TEXT_SIZE)
            }
        }
    }
}

@Composable
fun PlayHead(noteWidth: Dp, scrollState: ScrollState, offsetX: Dp = 0.dp) {
    val color = MaterialTheme.colorScheme.secondary
    val currentPosition = EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.timeSigNumerator * 16
    var playHeadPosition = noteWidth * currentPosition.toFloat() - scrollState.value.dp
    if (playHeadPosition.value < 0) return
    playHeadPosition += offsetX
    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp).offset(playHeadPosition - 8.dp, (-6).dp)
        .rotate(90F), tint = color)
    Box(Modifier.fillMaxHeight().width(1.dp).offset(playHeadPosition).background(color))
}