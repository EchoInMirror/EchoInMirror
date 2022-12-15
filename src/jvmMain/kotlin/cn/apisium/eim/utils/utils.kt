package cn.apisium.eim.utils

import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import java.util.*

fun lerp(start: Float, stop: Float, fraction: Float) = (1 - fraction) * start + fraction * stop
fun mapValue(value: Float, start1: Float, stop1: Float) = ((value - start1) / (stop1 - start1)).coerceIn(0f, 1f)

fun randomUUID() = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

data class Border(val strokeWidth: Dp, val color: Color)

@Composable
fun getSurfaceColor(elevation: Dp) = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + elevation)

@Stable
fun Modifier.border(
    start: Border? = null,
    top: Border? = null,
    end: Border? = null,
    bottom: Border? = null,
) =
    drawBehind {
        start?.let {
            drawStartBorder(it, shareTop = top != null, shareBottom = bottom != null)
        }
        top?.let {
            drawTopBorder(it, shareStart = start != null, shareEnd = end != null)
        }
        end?.let {
            drawEndBorder(it, shareTop = top != null, shareBottom = bottom != null)
        }
        bottom?.let {
            drawBottomBorder(border = it, shareStart = start != null, shareEnd = end != null)
        }
    }

private fun DrawScope.drawTopBorder(
    border: Border,
    shareStart: Boolean = true,
    shareEnd: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(if (shareStart) strokeWidthPx else 0f, strokeWidthPx)
            val width = size.width
            lineTo(if (shareEnd) width - strokeWidthPx else width, strokeWidthPx)
            lineTo(width, 0f)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawBottomBorder(
    border: Border,
    shareStart: Boolean,
    shareEnd: Boolean
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            val width = size.width
            val height = size.height
            moveTo(0f, height)
            lineTo(if (shareStart) strokeWidthPx else 0f, height - strokeWidthPx)
            lineTo(if (shareEnd) width - strokeWidthPx else width, height - strokeWidthPx)
            lineTo(width, height)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawStartBorder(
    border: Border,
    shareTop: Boolean = true,
    shareBottom: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(strokeWidthPx, if (shareTop) strokeWidthPx else 0f)
            val height = size.height
            lineTo(strokeWidthPx, if (shareBottom) height - strokeWidthPx else height)
            lineTo(0f, height)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawEndBorder(
    border: Border,
    shareTop: Boolean = true,
    shareBottom: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width, 0f)
            lineTo(width - strokeWidthPx, if (shareTop) strokeWidthPx else 0f)
            lineTo(width - strokeWidthPx, if (shareBottom) height - strokeWidthPx else height)
            lineTo(width, height)
            close()
        },
        color = border.color
    )
}
