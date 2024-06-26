package com.eimsound.daw.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.eimsound.audioprocessor.AudioProcessorParameter
import com.eimsound.daw.api.processor.DefaultHandledParameter
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.components.controllers.ParameterControllerComponent
import com.eimsound.daw.language.langs
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicAudioParameterView(parameters: List<AudioProcessorParameter>, uuid: UUID) {
    FlowRow(Modifier.fillMaxWidth().padding(8.dp, 4.dp, 8.dp), Arrangement.SpaceEvenly, maxItemsInEachRow = 3) {
        parameters.fastForEach { p ->
            key(p) { ParameterControllerComponent(p, uuid) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun FloatingLayerProvider.openParameterSelector(processor: TrackAudioProcessorWrapper) {
    val key = Any()
    var tmpSelectedParameter = ""
    val close: () -> Unit = { closeFloatingLayer(key) }
    openFloatingLayer(::closeFloatingLayer, key = key, hasOverlay = true) {
        Dialog({
            close()
            if (tmpSelectedParameter.isNotEmpty()) {
                processor.processor.parameters.firstOrNull {
                    "${it.name} (${it.id})" == tmpSelectedParameter
                }?.let {
                    processor.handledParameters += DefaultHandledParameter(it)
                }
            }
        }, close, modifier = Modifier.widthIn(300.dp, 800.dp)) {
            Text(langs.audioProcessorLangs.selectParameter, style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                processor.handledParameters.fastForEach {
                    key(it.parameter) {
                        InputChip(true, {
                            // TODO: convert to action
                            processor.handledParameters = processor.handledParameters.filter { p -> p != it }
                        }, { Text(it.parameter.name) },
                            trailingIcon = { Icon(Icons.Filled.Close, langs.delete) },
                        )
                    }
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                var selectedParameter by remember {
                    val lastModified = processor.processor.lastModifiedParameter
                    mutableStateOf(if (lastModified == null) "" else "${lastModified.name} (${lastModified.id})")
                }
                tmpSelectedParameter = selectedParameter
                DropdownSelector(
                    { selectedParameter = it },
                    remember(processor.handledParameters) {
                        val set = processor.handledParameters.mapTo(mutableSetOf()) { it.parameter }
                        processor.processor.parameters.mapNotNull { if (it in set) null else "${it.name} (${it.id})" }
                    },
                    selectedParameter, Modifier.weight(1F)
                )
                IconButton({
                    // TODO: convert to action
                    val cur = selectedParameter
                    selectedParameter = ""
                    tmpSelectedParameter = ""
                    processor.handledParameters += DefaultHandledParameter(processor.processor.parameters.firstOrNull {
                        "${it.name} (${it.id})" == cur
                    } ?: return@IconButton)
                }, 30.dp, Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Add, "添加")
                }
            }
        }
    }
}

@Composable
fun BasicAudioParameterView(processor: TrackAudioProcessorWrapper) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicAudioParameterView(processor.handledParameters.fastMap { it.parameter }, processor.processor.uuid)
        val floatingLayerProvider = LocalFloatingLayerProvider.current
        CustomButton({
            floatingLayerProvider.openParameterSelector(processor)
        }, Modifier.padding(bottom = 4.dp)) {
            Text(langs.audioProcessorLangs.selectParameter)
        }
    }
}
