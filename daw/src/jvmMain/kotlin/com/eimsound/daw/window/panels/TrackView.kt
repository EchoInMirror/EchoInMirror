package com.eimsound.daw.window.panels

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorEditor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.audioparameters.BasicAudioParameterView
import com.eimsound.daw.components.utils.clickableWithIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardHeader(p: TrackAudioProcessorWrapper, index: Int) {
//    val shape = MaterialTheme.shapes.small.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
//    val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)
    Surface(Modifier.clickableWithIcon(onClick = p.processor::onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1F).padding(horizontal = 12.dp)) {
                Text("$index.", Modifier.padding(end = 6.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(p.processor.name, Modifier.weight(1F), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CustomCheckbox(!p.isBypassed, { p.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AudioProcessorEditor(index: Int, p: TrackAudioProcessorWrapper) {
//    stickyHeader(p) { CardHeader(p, index) }
//    item(p.processor) {
//    val shape = MaterialTheme.shapes.small.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
    Surface(Modifier.fillMaxWidth().padding(8.dp), MaterialTheme.shapes.small,
        tonalElevation = 5.dp, shadowElevation = 2.dp) {
        Column {
            CardHeader(p, index)
            Divider()
            val processor = p.processor
            if (processor is AudioProcessorEditor) processor.Editor()
            else if (processor.parameters.isNotEmpty()) BasicAudioParameterView(p)
            else Text("未知的处理器: ${processor.name}", Modifier.padding(16.dp, 50.dp), textAlign = TextAlign.Center)
        }
    }
//    }
}

object TrackView : Panel {
    override val name = "轨道视图"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Default.ViewDay, name)
    }

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            val state = rememberLazyListState()
            val track = EchoInMirror.selectedTrack
            LazyColumn(state = state) {
                if (track != null) {
                    itemsIndexed(track.preProcessorsChain) { index, item ->
                        AudioProcessorEditor(index, item)
                    }
                    itemsIndexed(track.postProcessorsChain) { index, item ->
                        AudioProcessorEditor(index, item)
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}
