package com.eimsound.daw.components.audioparameters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.IAudioProcessorParameter
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.components.*
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.utils.range

@Composable
private fun ParameterSlider(p: IAudioProcessorParameter) {
    Column(Modifier.width(64.dp).padding(vertical = 4.dp)) {
        if (p.isFloat) Slider(p.value, { p.value = it }, valueRange = p.range)
        else Slider(p.value, { p.value = it }, valueRange = p.range, steps = p.range.range.toInt())
        Text(p.name, Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicAudioParameterView(parameters: List<IAudioProcessorParameter>, content: @Composable (() -> Unit)? = null) {
    FlowRow(Modifier.fillMaxWidth().padding(8.dp, 4.dp), Arrangement.SpaceEvenly) {
        parameters.fastForEach { p ->
            key(p) { ParameterSlider(p) }
        }
        content?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
private fun FloatingLayerProvider.openParameterSelector(processor: TrackAudioProcessorWrapper) {
    val key = Any()
    val close = { closeFloatingLayer(key) }
    openFloatingLayer(::closeFloatingLayer, key = key, hasOverlay = true) {
        Dialog(close, modifier = Modifier.widthIn(max = 460.dp)) {
            Text("选择参数", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                processor.handledParameters.fastForEach {
                    key(it.parameter) {
                        InputChip(true, {
                            // TODO
                        }, { Text(it.parameter.id) },
                            trailingIcon = { Icon(Icons.Filled.Close, "删除") },
                        )
                    }
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                var selectedParameter by remember { mutableStateOf("") }
                Selector(remember(processor.handledParameters) {
                    val set = processor.handledParameters.mapTo(mutableSetOf()) { it.parameter }
                    processor.processor.parameters.mapNotNull { if (it in set) null else "${it.name} (${it.id})" }
                }, selectedParameter, Modifier.weight(1F)) { selectedParameter = it }
                IconButton({ /* TODO */ }, 30.dp, Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Add, "添加")
                }
            }
        }
    }
}

@Composable
fun BasicAudioParameterView(processor: TrackAudioProcessorWrapper) {
    BasicAudioParameterView(processor.handledParameters.map { it.parameter }) {
        val floatingLayerProvider = LocalFloatingLayerProvider.current
        CustomButton({ floatingLayerProvider.openParameterSelector(processor) }) {
            Text("选择参数")
        }
    }
}
