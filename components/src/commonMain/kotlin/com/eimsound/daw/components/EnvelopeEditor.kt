package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.eimsound.audioprocessor.data.EnvelopePointList
import com.eimsound.daw.utils.binarySearch
import com.eimsound.daw.utils.coerceIn

@Composable
fun EnvelopeEditor(points: EnvelopePointList, start: Float, color: Color, valueRange: IntRange, noteWidth: MutableState<Dp>) {
    val lastIndex = remember { arrayOf(0) }
    Canvas(Modifier.fillMaxSize().pointerInput(points) {

    }) {
        val end = start + size.width / noteWidth.value.toPx()
        // binary search for start point
        points.read()
        val startIdx = (points.binarySearch { it.time < start } - 1).coerceAtLeast(0)
        lastIndex[0] = startIdx
        val range = valueRange.last - valueRange.first

        // draw line
        for (i in startIdx until points.size) {
            val cur = points[i]
            if (cur.time > end) break
            val next = points.getOrNull(i + 1)
            drawLine(color,
                Offset((cur.time - start) * noteWidth.value.toPx(), size.height * (1 - cur.value.coerceIn(valueRange) / range)),
                Offset(
                    if (next == null) size.width else (next.time - start) * noteWidth.value.toPx(),
                    size.height * (1 - (next ?: cur).value.coerceIn(valueRange) / range)
                ),
                2f
            )
        }

        // draw points
        for (i in startIdx until points.size) {
            val cur = points[i]
            if (cur.time > end) break
            drawCircle(color, 4F,
                Offset(
                    (cur.time - start) * noteWidth.value.toPx(),
                    size.height * (1 - cur.value.coerceIn(valueRange) / range)
                )
            )
        }
    }
}
