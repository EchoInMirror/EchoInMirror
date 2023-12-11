package com.eimsound.daw.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.components.utils.warning
import com.eimsound.daw.commons.ExperimentalEIMApi
import com.eimsound.daw.api.LevelMeter

// Math.sqrt(10.0 ** (value / 20)) / 1.4
val levelMarks1_40 = arrayOf(
    0.71428573F,
    0.50567555F,
    0.3579909F,
)

@OptIn(ExperimentalEIMApi::class)
@Composable
fun Level(
    peak: LevelMeter,
    modifier: Modifier = Modifier.height(80.dp),
    stroke: Dp = 3.dp,
    gapWidth: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    colorTrack: Color = color.copy(alpha = 0.3f)
) {
    val rightX = stroke + gapWidth
    val left = peak.left
    val right = peak.right
    val warningColor = MaterialTheme.colorScheme.warning
    val markColor = color.copy(0.5F)
    val leftColor by animateColorAsState(left.getLevelColor(color, color, warningColor), tween(200))
    val rightColor by animateColorAsState(right.getLevelColor(color, color, warningColor), tween(200))
    val aniLeft by animateFloatAsState(left.toDisplayPercentage())
    val aniRight by animateFloatAsState(right.toDisplayPercentage())
    val markColorLighter = colorTrack.compositeOver(MaterialTheme.colorScheme.surface).copy(0.4F)
    Canvas(modifier.width(stroke * 2 + gapWidth)) {
        val height = size.height
        val strokePx = stroke.toPx()
        val rightXDp = rightX.toPx()
        drawLine(colorTrack, Offset(0f, 0f), Offset(0f, height), strokePx, StrokeCap.Round)
        if (aniLeft > 0.0001F) drawLine(leftColor,
            Offset(0f, height * (1F - aniLeft)),
            Offset(0f, height), strokePx, StrokeCap.Round)
        drawLine(colorTrack, Offset(rightXDp, 0f), Offset(rightXDp, size.height), strokePx, StrokeCap.Round)
        if (aniRight > 0.0001F) drawLine(rightColor,
            Offset(rightXDp, height * (1F - aniRight)),
            Offset(rightXDp, height), strokePx, StrokeCap.Round)
        levelMarks1_40.forEach {
            val y = height * (1 - it)
            drawCircle(if (aniLeft > it) markColorLighter else markColor, strokePx / 2, Offset(0F, y))
            drawCircle(if (aniRight > it) markColorLighter else markColor, strokePx / 2, Offset(rightXDp, y))
        }
    }
}

val levelMarks = listOf(
    100F to "0",
    70.79458F to "-6",
    50.118725F to "-12"
)

@Composable
fun LevelHints(
    modifier: Modifier = Modifier.height(80.dp),
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
    alpha: Float = 0.8f
) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val top = size.height * 2 / 7
        val height = size.height * 5 / 7
        levelMarks.fastForEach { (value, label) ->
            val y = height * (100F - value) / 100F + top
            val result = measurer.measure(
                AnnotatedString(label),
                style,
            )
            drawText(result, color, Offset(size.width / 2 - result.size.width / 2 - size.width / 10, y - result.size.height / 2), alpha)
        }
    }
}
