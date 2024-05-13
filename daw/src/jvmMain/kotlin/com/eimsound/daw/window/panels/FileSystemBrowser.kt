package com.eimsound.daw.window.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audiosources.AudioSourceManager
import com.eimsound.dsp.data.midi.getNotes
import com.eimsound.dsp.data.midi.toMidiTracks
import com.eimsound.daw.Configuration
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.trees.FileNode
import com.eimsound.daw.components.trees.Tree
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.language.langs
import com.eimsound.daw.processor.PreviewerAudioProcessor
import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Path
import javax.sound.midi.MidiSystem
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.name

val fileBrowserPreviewer = PreviewerAudioProcessor(AudioProcessorManager.instance.eimAudioProcessorFactory)

object FileSystemBrowser : Panel {
    override val name get() = langs.fileBrowser
    override val direction = PanelDirection.Vertical
    private val roots by mutableStateOf(run {
        val roots = File.listRoots().toMutableList()
        roots += SystemUtils.getUserDir()
        if (SystemUtils.IS_OS_MAC) {
            val home = SystemUtils.getUserHome()
            if (home != null) roots += home
            roots += File("/Volumes")
        }
        roots.map { it.toPath() }
    })
    private var component: (@Composable BoxScope.() -> Unit)? by mutableStateOf(null)
    private var nodeName: String? by mutableStateOf(null)
    private var autoPlay by mutableStateOf(true)

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.FolderOpen, name)
    }

    @Composable
    override fun Content() {
        Column {
            Tree(Modifier.weight(1F), { if (it is Path) createPreviewerComponent(it) }) {
                roots.fastForEach {
                    key(it) { FileNode(it, showSupFormatOnly = Configuration.fileBrowserShowSupFormatOnly) }
                }
                Configuration.fileBrowserCustomRoots.forEach {
                    key(it) { FileNode(it, showSupFormatOnly = Configuration.fileBrowserShowSupFormatOnly) }
                }
            }
            Previewer()
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
        Spacer(
            Modifier.width(if (isHovered) 4.dp else 2.dp).fillMaxHeight()
                .graphicsLayer(translationX = left)
                .hoverable(interactionSource)
                .background(color)
        )
        Spacer(Modifier.width(leftDp).fillMaxHeight().background(color.copy(0.14F)))
    }

    @Composable
    private fun Previewer() {
        val width = remember { intArrayOf(1) }
        Surface(Modifier.fillMaxWidth().height(40.dp).onPlaced { width[0] = it.size.width }, tonalElevation = 3.dp) {
            @OptIn(ExperimentalFoundationApi::class)
            DropdownMenu({ close ->
                MenuItem({
                    autoPlay = !autoPlay
                    fileBrowserPreviewer.position.isPlaying = autoPlay
                    close()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(langs.autoPlay)
                    Filled()
                    Icon(
                        if (autoPlay) Icons.Outlined.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank, langs.autoPlay
                    )
                }
                MenuItem({
                    fileBrowserPreviewer.position.isProjectLooping = !fileBrowserPreviewer.position.isProjectLooping
                    close()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(langs.loopPlay)
                    Filled()
                    Icon(
                        if (fileBrowserPreviewer.position.isProjectLooping) Icons.Outlined.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank, langs.loopPlay
                    )
                }
                MenuItem(modifier = Modifier.fillMaxWidth()) {
                    Text(langs.ccEvents.volume)
                    Filled()
                    Slider(fileBrowserPreviewer.volume, { fileBrowserPreviewer.volume = it }, Modifier.width(140.dp))
                }
                Divider()
                MenuItem(close, enabled = false, modifier = Modifier.fillMaxSize()) {
                    Text(nodeName ?: langs.pleaseSelectFile, overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
            }, enabled = false, matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                val c = component
                if (c == null) Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(langs.pleaseSelectFile, style = MaterialTheme.typography.labelMedium)
                } else Box {
                    val draggableState = remember {
                        DraggableState { fileBrowserPreviewer.playPosition += it / width[0].toDouble() }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(Modifier.padding(horizontal = 4.dp), content = c)
                    Box(Modifier.fillMaxSize()
                        .draggable(
                            draggableState, Orientation.Horizontal, interactionSource = interactionSource,
                            onDragStopped = {
                                fileBrowserPreviewer.position.isPlaying = true
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures({
                                fileBrowserPreviewer.position.isProjectLooping =
                                    !fileBrowserPreviewer.position.isProjectLooping
                            }) {
                                fileBrowserPreviewer.playPosition = it.x / width[0].toDouble()
                                fileBrowserPreviewer.position.isPlaying = true
                            }
                        }
                        .pointerHoverIcon(PointerIcon.HorizontalResize)
                    ) {
                        PlayHead(interactionSource, width)
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun createPreviewerComponent(file: Path) {
        GlobalScope.launch {
            if (file.isDirectory()) return@launch
            val ext = file.extension.lowercase()
            try {
                if (ext == "mid") {
                    val notes = withContext(Dispatchers.IO) {
                        MidiSystem.getSequence(file.inputStream()).toMidiTracks(EchoInMirror.currentPosition.ppq).getNotes()
                    }
                    component = { MidiView(notes) }
                    nodeName = file.name
                    fileBrowserPreviewer.setPreviewTarget(notes)
                    if (autoPlay) fileBrowserPreviewer.position.isPlaying = true
                } else if (AudioSourceManager.supportedFormats.contains(ext)) {
                    val audioSource = AudioSourceManager.createProxyFileSource(file)
                    component = null
                    EchoInMirror.audioThumbnailCache.get(file, audioSource) { audioThumbnail, _ ->
                        audioSource.position = 0
                        fileBrowserPreviewer.setPreviewTarget(audioSource)
                        if (audioThumbnail != null) component = { Waveform(audioThumbnail) }
                        nodeName = file.name
                        if (autoPlay) fileBrowserPreviewer.position.isPlaying = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                component = null
                nodeName = null
                fileBrowserPreviewer.clear()
            }
        }
    }
}
