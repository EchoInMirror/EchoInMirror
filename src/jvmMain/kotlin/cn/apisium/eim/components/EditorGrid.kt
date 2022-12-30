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
    val beatsOutlineColor = MaterialTheme.colorScheme.surfaceVariant
    val stepsOutlineColor = beatsOutlineColor.copy(0.5F)
    val barsOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val timeSigDenominator = EchoInMirror.currentPosition.timeSigDenominator
    val timeSigNumerator = EchoInMirror.currentPosition.timeSigNumerator
    val ppq = EchoInMirror.currentPosition.ppq
    Canvas(modifier) {
        val noteWidthPx = noteWidth.value.toPx()
        val horizontalScrollValue = horizontalScrollState.value
        val beatsWidth = noteWidthPx * ppq
        val drawBeats = noteWidthPx > 0.1F
        val drawSteps = noteWidthPx > 0.4F
        var horizontalDrawWidth = if (drawBeats) beatsWidth else beatsWidth * timeSigNumerator
        var highlightBarsWidth = if (drawBeats) timeSigDenominator else timeSigDenominator * timeSigNumerator
        val highlightStepsWidth = if (drawSteps) timeSigNumerator else 1
        if (drawSteps) {
            horizontalDrawWidth /= timeSigNumerator
            highlightBarsWidth *= timeSigNumerator
        }
        for (i in (horizontalScrollValue / horizontalDrawWidth).toInt()..
                ((horizontalScrollValue + size.width) / horizontalDrawWidth).toInt()) {
            val x = i * horizontalDrawWidth - horizontalScrollState.value
            drawLine(if (i % highlightBarsWidth == 0) barsOutlineColor else
                if (i % highlightStepsWidth == 0) beatsOutlineColor else stepsOutlineColor, Offset(x, topPadding ?: 0F),
                Offset(x, size.height), 1F)
        }
    }
}