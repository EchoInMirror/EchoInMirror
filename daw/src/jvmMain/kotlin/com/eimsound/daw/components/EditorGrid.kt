package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import com.eimsound.daw.EchoInMirror

@Composable
fun EditorGrid(
    noteWidth: MutableState<Dp>,
    horizontalScrollState: ScrollState,
    range: IntRange? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val beatsOutlineColor = MaterialTheme.colorScheme.surfaceVariant
    val stepsOutlineColor = beatsOutlineColor.copy(0.5F)
    val barsOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val eightBarsOutlineColor = MaterialTheme.colorScheme.outline.copy(0.7F)
    val timeSigDenominator = EchoInMirror.currentPosition.timeSigDenominator
    val timeSigNumerator = EchoInMirror.currentPosition.timeSigNumerator
    val ppq = EchoInMirror.currentPosition.ppq
    val rangeColor = MaterialTheme.colorScheme.primary.copy(0.07F)
    Canvas(modifier) {
        val noteWidthPx = noteWidth.value.toPx()
        val horizontalScrollValue = horizontalScrollState.value

        if (range != null) {
            val rangeStart = range.first * noteWidthPx
            val rangeEnd = range.last * noteWidthPx
            drawRect(rangeColor,
                Offset(rangeStart - horizontalScrollValue, 0F),
                Size(rangeEnd - rangeStart, size.height)
            )
        }

        val beatsWidth = noteWidthPx * ppq
        val drawBeats = noteWidthPx > 0.1F
        val drawSteps = noteWidthPx > 0.4F
        var horizontalDrawWidth = if (drawBeats) beatsWidth else beatsWidth * timeSigNumerator
        var highlightBarsWidth = if (drawBeats) timeSigDenominator else timeSigDenominator * timeSigNumerator
        val highlightStepsWidth = if (drawSteps) timeSigNumerator else 1
        var eightBarsWidth = 8
        if (drawSteps) {
            horizontalDrawWidth /= timeSigNumerator
            highlightBarsWidth *= timeSigNumerator
            eightBarsWidth = highlightBarsWidth * 8
        } else if (drawBeats) eightBarsWidth = highlightBarsWidth * 8
        for (i in (horizontalScrollValue / horizontalDrawWidth).toInt()..
                ((horizontalScrollValue + size.width) / horizontalDrawWidth).toInt()) {
            val x = i * horizontalDrawWidth - horizontalScrollState.value
            drawLine(if (i % highlightBarsWidth == 0) {
                if (i % eightBarsWidth == 0) eightBarsOutlineColor else barsOutlineColor
            } else
                if (i % highlightStepsWidth == 0) beatsOutlineColor else stepsOutlineColor, Offset(x, 0F),
                Offset(x, size.height), 1F)
        }
    }
}