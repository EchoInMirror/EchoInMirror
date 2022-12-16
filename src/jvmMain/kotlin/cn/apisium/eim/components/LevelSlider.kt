package cn.apisium.eim.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import cn.apisium.eim.api.processor.dsp.Volume
import cn.apisium.eim.components.silder.DefaultTrack
import cn.apisium.eim.components.silder.Slider
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun VolumeSlider(volume: Volume, modifier: Modifier = Modifier.height(150.dp), isVertical: Boolean = true) {
    Slider(
        (sqrt(volume.volume) * 100F).let { if (isVertical) 140 - it else it },
        { volume.volume = ((if (isVertical) 140 - it else it) / 100F).pow(2) },
        valueRange = 0f..140f,
        modifier = modifier,
        thumbSize = DpSize(14.dp, 14.dp),
        isVertical = isVertical,
        track = { m, progress, interactionSource, tickFractions, enabled, i ->
            DefaultTrack(
                m,
                progress,
                interactionSource,
                tickFractions,
                enabled,
                i,
                startPoint = if (isVertical) 1f else 0f,
                stroke = 3.dp
            )
        }
    )
}
