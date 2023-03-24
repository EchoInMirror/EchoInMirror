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
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eimsound.daw.components.utils.warning
import com.eimsound.daw.utils.ExperimentalEIMApi
import com.eimsound.daw.utils.LevelMeter

@OptIn(ExperimentalEIMApi::class)
@Composable
fun Level(
    peak: LevelMeter,
    modifier: Modifier = Modifier.height(80.dp),
    stroke: Float = 5f,
    gapWidth: Float = 4f,
    color: Color = MaterialTheme.colorScheme.primary,
    colorTrack: Color = color.copy(alpha = 0.3f)
) {
    val rightX = stroke + gapWidth
    val left = peak.left
    val right = peak.right
    val warningColor = MaterialTheme.colorScheme.warning
    val leftColor by animateColorAsState(left.getLevelColor(color, color, warningColor), tween(200))
    val rightColor by animateColorAsState(right.getLevelColor(color, color, warningColor), tween(200))
    val aniLeft by animateFloatAsState(left.toDisplayPercentage())
    val aniRight by animateFloatAsState(right.toDisplayPercentage())
    Canvas(modifier.width((stroke * 2 + gapWidth).dp)) {
        val height = size.height
        drawLine(colorTrack, Offset(0f, 0f), Offset(0f, height), stroke, StrokeCap.Round)
        if (aniLeft > 0.0001F) drawLine(leftColor,
            Offset(0f, height * (1F - aniLeft)),
            Offset(0f, height), stroke, StrokeCap.Round)
        drawLine(colorTrack, Offset(rightX, 0f), Offset(rightX, size.height), stroke, StrokeCap.Round)
        if (aniRight > 0.0001F) drawLine(rightColor,
            Offset(rightX, height * (1F - aniRight)),
            Offset(rightX, height), stroke, StrokeCap.Round)
    }
}

val levelMarks = listOf(
    100F to "0",
    70.79458F to "-6",
    50.118725F to "-12"
)

@OptIn(ExperimentalTextApi::class)
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
        levelMarks.forEach { (value, label) ->
            val y = height * (100F - value) / 100F + top
            val result = measurer.measure(
                AnnotatedString(label),
                style,
            )
            drawText(result, color, Offset(size.width / 2 - result.size.width / 2 - size.width / 10, y - result.size.height / 2), alpha)
        }
    }
}
