package com.eimsound.daw.window.panels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.audioprocessor.AudioProcessorEditor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection

object TrackView : Panel {
    override val name = "轨道视图"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Default.ViewDay, name)
    }

    private fun LazyListScope.audioProcessorEditors(list: MutableList<AudioProcessor>) {
        items(list, { it }) {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                if (it is AudioProcessorEditor) it.Editor()
                else Text("未知的处理器: ${it.name}", Modifier.padding(16.dp, 50.dp), textAlign = TextAlign.Center)
            }
        }
    }

    @Composable
    override fun Content() {
        val track = EchoInMirror.selectedTrack ?: return
        LazyColumn {
            audioProcessorEditors(track.preProcessorsChain)
            audioProcessorEditors(track.postProcessorsChain)
        }
    }
}
