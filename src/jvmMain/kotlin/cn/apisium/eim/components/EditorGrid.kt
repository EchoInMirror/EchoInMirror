package cn.apisium.eim.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import cn.apisium.eim.EchoInMirror

@Composable
fun EditorGrid(
    noteWidth: MutableState<Dp>,
    horizontalScrollState: ScrollState,
    topPadding: Float? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val outlineColor = MaterialTheme.colorScheme.surfaceVariant
    val barsOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val timeSigDenominator = EchoInMirror.currentPosition.timeSigDenominator
    val timeSigNumerator = EchoInMirror.currentPosition.timeSigNumerator
    val ppq = EchoInMirror.currentPosition.ppq
    Canvas(modifier) {
        val noteWidthPx = noteWidth.value.toPx()
        val horizontalScrollValue = horizontalScrollState.value
        val beatsWidth = noteWidthPx * ppq
        val drawBeats = noteWidthPx > 0.1F
        val horizontalDrawWidth = if (drawBeats) beatsWidth else beatsWidth * timeSigNumerator
        val highlightWidth = if (drawBeats) timeSigDenominator else timeSigDenominator * timeSigNumerator
        for (i in (horizontalScrollValue / horizontalDrawWidth).toInt()..((horizontalScrollValue + size.width) / horizontalDrawWidth).toInt()) {
            val x = i * horizontalDrawWidth - horizontalScrollState.value
            drawLine(if (i % highlightWidth == 0) barsOutlineColor else outlineColor, Offset(x, topPadding ?: 0F),
                Offset(x, size.height), 1F)
        }
    }
}