package com.eimsound.daw.window.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.bonsai.core.node.Node
import cafe.adriel.bonsai.filesystem.FileSystemTree
import com.eimsound.audioprocessor.AudioSourceManager
import com.eimsound.audioprocessor.data.midi.getMidiEvents
import com.eimsound.audioprocessor.data.midi.getNoteMessages
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.FileSystemStyle
import com.eimsound.daw.components.MidiView
import com.eimsound.daw.components.Tree
import com.eimsound.daw.components.Waveform
import com.eimsound.daw.components.dragdrop.FileDraggable
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import java.io.File
import java.util.*
import javax.sound.midi.MidiSystem

val FileMapper = @Composable { node: Node<Path>, content: @Composable () -> Unit ->
    if (FileSystem.SYSTEM.metadata(node.content).isDirectory) content()
    else FileDraggable(node.content.toFile()) { content() }
}

object FileSystemBrowser: Panel {
    override val name = "浏览器"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.FolderOpen, name)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    override fun Content() {
        Column {
            var component by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
            Tree(FileSystemTree(File("C:\\Python311"), true), FileSystemStyle, FileMapper, Modifier.weight(1F)) {
                if (FileSystem.SYSTEM.metadata(it.content).isDirectory) return@Tree
                val ext = it.content.toFile().extension.lowercase()
                if (ext == "mid") {
                    GlobalScope.launch {
                        val list = getNoteMessages(withContext(Dispatchers.IO) {
                            MidiSystem.getSequence(it.content.toFile())
                        }.getMidiEvents(1))
                        component = { MidiView(list) }
                    }
                } else if (AudioSourceManager.instance.supportedFormats.contains(ext)) {
                    GlobalScope.launch {
                        EchoInMirror.audioThumbnailCache[it.content.toNioPath()]?.let { a -> component = { Waveform(a) } }
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().height(40.dp), tonalElevation = 3.dp) {
                Box(Modifier.padding(horizontal = 4.dp)) { component?.invoke() }
            }
        }
    }
}
