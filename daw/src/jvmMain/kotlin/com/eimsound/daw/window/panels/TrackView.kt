package com.eimsound.daw.window.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.audioprocessor.AudioProcessorEditor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.audioparameters.BasicAudioParameterView

object TrackView : Panel {
    override val name = "轨道视图"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Default.ViewDay, name)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun LazyListScope.audioProcessorEditors(p: TrackAudioProcessorWrapper, index: Int) {
        stickyHeader(p) {
            val shape = MaterialTheme.shapes.small.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
            val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)
            Surface(Modifier.fillMaxWidth().clipToBounds().background(backgroundColor), shape, tonalElevation = 5.dp, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1F).padding(horizontal = 12.dp)) {
                        Text("$index.", Modifier.padding(end = 8.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(p.processor.name, Modifier.weight(1F), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CustomCheckbox(!p.isBypassed, { p.isBypassed = it }, Modifier.padding(start = 8.dp))
                }
            }
        }
        item(p.processor) {
            val shape = MaterialTheme.shapes.small.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
            Surface(Modifier.fillMaxWidth().clip(shape), shape, tonalElevation = 5.dp, shadowElevation = 2.dp) {
                val processor = p.processor
                if (processor is AudioProcessorEditor) processor.Editor()
                else if (processor.parameters.isNotEmpty()) BasicAudioParameterView(processor.parameters)
                else Text("未知的处理器: ${processor.name}", Modifier.padding(16.dp, 50.dp), textAlign = TextAlign.Center)
            }
        }
    }

    @Composable
    override fun Content() {
        val track = EchoInMirror.selectedTrack ?: return
        LazyColumn(Modifier.fillMaxSize().clip(MaterialTheme.shapes.small).padding(8.dp)) {
            track.preProcessorsChain.fastForEachIndexed { i, it -> audioProcessorEditors(it, i) }
            track.postProcessorsChain.fastForEachIndexed { i, it -> audioProcessorEditors(it, i) }
        }
    }
}
