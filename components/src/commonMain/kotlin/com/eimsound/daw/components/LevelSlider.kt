package com.eimsound.daw.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.dsp.Volume
import com.eimsound.daw.components.silder.DefaultTrack
import com.eimsound.daw.components.silder.Slider
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun VolumeSlider(volume: Volume, modifier: Modifier = Modifier.height(150.dp), isVertical: Boolean = true) {
    Slider(
        (sqrt(volume.volume) * 100F).let { if (isVertical) 140 - it else it },
        { volume.volume = ((if (isVertical) 140 - it else it) / 100F).pow(2) },
        valueRange = 0f..140f,
        modifier = modifier,
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
