package com.eimsound.daw.components.audioparameters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.IAudioProcessorParameter
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.utils.range
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment

@Composable
private fun ParameterSlider(p: IAudioProcessorParameter) {
    Column(Modifier.width(64.dp)) {
        if (p.isFloat) Slider(p.value, { p.value = it }, valueRange = p.range)
        else Slider(p.value, { p.value = it }, valueRange = p.range, steps = p.range.range.toInt())
        Text(p.name, Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BasicAudioParameterView(parameters: List<IAudioProcessorParameter>) {
    FlowRow(Modifier.fillMaxWidth().padding(8.dp), mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
        crossAxisSpacing = 8.dp) {
        parameters.fastForEach { p ->
            key(p) { ParameterSlider(p) }
        }
    }
}
