package cn.apisium.eim.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import cn.apisium.eim.api.processor.LevelPeak
import kotlin.math.*

@Composable
fun Level(
    peak: LevelPeak,
    modifier: Modifier = Modifier.height(80.dp),
    stroke: Float = 5f,
    gapWidth: Float = 4f,
    color: Color = MaterialTheme.colorScheme.primary,
    colorTrack: Color = color.copy(alpha = 0.3f)
) {
    val aniLeft by animateFloatAsState(peak.left)
    val aniRight by animateFloatAsState(peak.right)
    val rightX = stroke + gapWidth
    Canvas(modifier.width((stroke * 2 + gapWidth).dp)) {
        val height = size.height
        drawLine(colorTrack, Offset(0f, 0f), Offset(0f, height), stroke, StrokeCap.Round)
        if (aniLeft != 0f) drawLine(color, Offset(0f, (1 - (sqrt(aniLeft) / 1.4f).coerceAtMost(1f)) * height), Offset(0f, height), stroke, StrokeCap.Round)
        drawLine(colorTrack, Offset(rightX, 0f), Offset(rightX, size.height), stroke, StrokeCap.Round)
        if (aniRight != 0f) drawLine(color, Offset(rightX, (1 - (sqrt(aniRight) / 1.4f).coerceAtMost(1f)) * height), Offset(rightX, height), stroke, StrokeCap.Round)
    }
}
