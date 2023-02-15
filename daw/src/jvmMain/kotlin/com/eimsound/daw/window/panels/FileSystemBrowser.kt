package com.eimsound.daw.window.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.bonsai.core.node.Node
import cafe.adriel.bonsai.filesystem.FileSystemTree
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.AudioSourceManager
import com.eimsound.audioprocessor.data.midi.parse
import com.eimsound.audioprocessor.data.midi.toMidiEvents
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.FileDraggable
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.processor.PreviewerAudioProcessor
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import java.io.File
import javax.sound.midi.MidiSystem

val FileMapper = @Composable { node: Node<Path>, content: @Composable () -> Unit ->
    if (FileSystem.SYSTEM.metadata(node.content).isDirectory) content()
    else FileDraggable(node.content.toFile()) { content() }
}

val fileBrowserPreviewer = PreviewerAudioProcessor(AudioProcessorManager.instance.eimAudioProcessorFactory)

object FileSystemBrowser: Panel {
    override val name = "文件浏览"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.FolderOpen, name)
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        Column {
            var component by remember { mutableStateOf<(@Composable BoxScope.() -> Unit)?>(null) }
            Tree(FileSystemTree(File("C:\\"), true), FileSystemStyle, FileMapper, Modifier.weight(1F)) {
                if (FileSystem.SYSTEM.metadata(it.content).isDirectory) return@Tree
                component = null
                val ext = it.content.toFile().extension.lowercase()
                GlobalScope.launch {
                    var hasContent = false
                    try {
                        if (ext == "mid") {
                            val list = withContext(Dispatchers.IO) {
                                MidiSystem.getSequence(it.content.toFile()).toMidiEvents().parse()
                            }
                            component = { MidiView(list.notes) }
                            fileBrowserPreviewer.setPreviewTarget(list.notes)
                            hasContent = true
                        } else if (AudioSourceManager.instance.supportedFormats.contains(ext)) {
                            val file = it.content.toNioPath()
                            val audioSource = AudioSourceManager.instance.createAudioSource(file)
                            EchoInMirror.audioThumbnailCache[file, audioSource]?.let {
                                component = { Waveform(it) }
                                hasContent = true
                            }
                            fileBrowserPreviewer.setPreviewTarget(audioSource)
                        }
                    } catch (e: Exception) {
                        hasContent = false
                        e.printStackTrace()
                    }
                    if (!hasContent) {
                        component = null
                        fileBrowserPreviewer.clear()
                    }
                }
            }
            val width = remember { intArrayOf(1) }
            Surface(Modifier.fillMaxWidth().height(40.dp).onGloballyPositioned { width[0] = it.size.width }, tonalElevation = 3.dp) {
                DropdownMenu({ close ->
                    MenuItem({
                        close()
                        fileBrowserPreviewer.position.isPlaying = !fileBrowserPreviewer.position.isPlaying
                    }) {
                        Text("自动播放")
                        Filled()
                        Icon(if (fileBrowserPreviewer.position.isPlaying) Icons.Filled.CheckBox
                        else Icons.Filled.CheckBoxOutlineBlank, "自动播放", DEFAULT_ICON_MODIFIER)
                    }
                    MenuItem({
                        close()
                        fileBrowserPreviewer.position.isProjectLooping = !fileBrowserPreviewer.position.isProjectLooping
                    }) {
                        Text("循环播放")
                        Filled()
                        Icon(if (fileBrowserPreviewer.position.isProjectLooping) Icons.Filled.CheckBox
                        else Icons.Filled.CheckBoxOutlineBlank, "自动播放", DEFAULT_ICON_MODIFIER)
                    }
                    Row {
                        Text("音量")
                        Slider(fileBrowserPreviewer.volume, { fileBrowserPreviewer.volume = it })
                    }
                }) {
                    val c = component
                    if (c == null) Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("请选择文件...", style = MaterialTheme.typography.labelMedium)
                    } else Box {
                        val draggableState = remember {
                            DraggableState { fileBrowserPreviewer.playPosition += it / width[0].toDouble() }
                        }
                        val interactionSource = remember { MutableInteractionSource() }
                        Box(Modifier.padding(horizontal = 4.dp), content = c)
                        Box(Modifier.fillMaxSize()
                            .draggable(draggableState, Orientation.Horizontal, interactionSource = interactionSource)
                            .pointerInput(Unit) {
                                detectTapGestures({
                                    fileBrowserPreviewer.position.isProjectLooping = !fileBrowserPreviewer.position.isProjectLooping
                                }) { fileBrowserPreviewer.playPosition = it.x / width[0].toDouble() }
                            }
                            .pointerHoverIcon(PointerIconDefaults.HorizontalResize)
                        ) {
                            PlayHead(interactionSource, width)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlayHead(interactionSource: MutableInteractionSource, width: IntArray) {
        val isHovered by interactionSource.collectIsHoveredAsState()
        val left: Float
        val leftDp = LocalDensity.current.run {
            left = (fileBrowserPreviewer.playPosition * width[0]).toFloat() - (if (isHovered) 2 else 1) * density
            left.toDp()
        }
        val color = MaterialTheme.colorScheme.primary
        Spacer(Modifier.width(if (isHovered) 4.dp else 2.dp).fillMaxHeight()
            .graphicsLayer(translationX = left)
            .hoverable(interactionSource)
            .background(color)
        )
        Spacer(Modifier.width(leftDp).fillMaxHeight().background(color.copy(0.14F)))
    }
}
