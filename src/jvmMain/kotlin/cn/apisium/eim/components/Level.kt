package cn.apisium.eim.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import cn.apisium.eim.api.processor.LevelMeter

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
    val aniLeft = peak.left
    val aniRight = peak.right
    Canvas(modifier.width((stroke * 2 + gapWidth).dp)) {
        val height = size.height
        drawLine(colorTrack, Offset(0f, 0f), Offset(0f, height), stroke, StrokeCap.Round)
        if (aniLeft.isNotZero()) drawLine(color, Offset(0f, height * (1F - aniLeft.toDisplayPercentage())), Offset(0f, height), stroke, StrokeCap.Round)
        drawLine(colorTrack, Offset(rightX, 0f), Offset(rightX, size.height), stroke, StrokeCap.Round)
        if (aniRight.isNotZero()) drawLine(color, Offset(rightX, height * (1F - aniRight.toDisplayPercentage())), Offset(rightX, height), stroke, StrokeCap.Round)
    }
}
