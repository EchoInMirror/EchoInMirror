package com.eimsound.daw.window.panels

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.bonsai.core.node.Node
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.data.midi.*
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.FileDraggable
import com.eimsound.daw.components.dragdrop.GlobalDraggable
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.processor.PreviewerAudioProcessor
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import org.apache.commons.lang3.SystemUtils
import java.io.File
import javax.sound.midi.MidiSystem
import com.eimsound.daw.components.IconButton as EIMIconButton

val FileMapper = @Composable { node: Node<Path>, content: @Composable () -> Unit ->
    if (FileSystem.SYSTEM.metadata(node.content).isDirectory) content()
    else FileDraggable(node.content.toFile()) { content() }
}

val fileBrowserPreviewer = PreviewerAudioProcessor(AudioProcessorManager.instance.eimAudioProcessorFactory)


//@Composable
//fun DraggableFile(file: File) {
//    var isDragging by remember { mutableStateOf(false) }
//    val shadowColor = if (isDragging) Color.LightGray else Color.Transparent
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(shadowColor, shape = RectangleShape)
//            .run {
//                if (isDragging) {
//                    this.border(1.dp, Color.Black, shape = RectangleShape)
//                } else {
//                    this
//                }
//            }
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { isDragging = true },
//                    onDragEnd = { isDragging = false }
//                )
//            }
//    ) {
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Icon(imageVector = Icons.Filled.FileCopy, contentDescription = null)
//            Text(file.name)
//            Spacer(modifier = Modifier.weight(1f))
//        }
//    }
//}


@Composable
fun MidiNode(file: File, indent: Int) {
    var expanded by remember { mutableStateOf(false) }
    GlobalDraggable( { file } ) {
        FileTreeRow(
            FileExtensionIcons["mid"]!!,
            file.name,
            hasButton = true,
            buttonState = expanded,
            onButtonClick = { expanded = !expanded },
            indent = indent
        )
    }

    if (expanded) {
        val midiTracks = MidiSystem.getSequence(file).toMidiTracks()
        midiTracks.forEachIndexed { index, midiTrack ->
            GlobalDraggable( { midiTrack } ) {
                FileTreeRow(
                    FileExtensionIcons["midiTrack"]!!,
                    midiTrack.name ?: "轨道 $index",
                    indent = indent + 1
                )
            }
        }
    }
}

@Composable
fun DictionaryNode(file: File, indent: Int) {
    var expanded by remember { mutableStateOf(false) }
    FileTreeRow(
        Icons.Filled.Folder,
        file.name,
        hasButton = file.listFiles()!!.isNotEmpty(),
        buttonState = expanded,
        onButtonClick = { expanded = !expanded },
        indent = indent
    )
    if (expanded) {
        for (child in file.listFiles()!!.sorted().sortedBy { it.isFile }) {
            FileTree(child, indent + 1)
        }
    }
}


@Composable
fun FileNode(file: File, indent: Int) {
    GlobalDraggable(
        { file },
    ) {
        FileTreeRow(
            FileExtensionIcons.getOrDefault(file.extension, Icons.Outlined.DevicesOther),
            file.name,
            indent = indent
        )
    }
}

@Composable
fun FileTreeRow(
    icon: ImageVector,
    text: String,
    hasButton: Boolean = false,
    buttonState: Boolean = false,
    onButtonClick: () -> Unit = {},
    indent: Int = 0,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    Row(
        modifier = Modifier.padding(start = (10 * (indent + 1)).dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        Icon(
            icon,
            text,
            modifier = Modifier.size(20.dp, 20.dp)
        )
        Text(text, modifier = Modifier.padding(start = 4.dp))
        if (hasButton) {
            EIMIconButton(
                onClick = onButtonClick,
                modifier = Modifier.padding(end = 4.dp).size(20.dp, 20.dp)
            ) {
                Icon(
                    if (buttonState) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun FileTree(file: File, indent: Int = 0) {
    when (file.extension) {
        "mid" -> MidiNode(file, indent)
        else -> {
            if (file.isFile) {
                FileNode(file, indent)
            } else {
                DictionaryNode(file, indent)
            }
        }
    }
}

@Composable
fun FileTreeUI(filePath: File) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    ) {
        FileTree(filePath)
    }
}

object FileSystemBrowser : Panel {
    override val name = "文件浏览"
    override val direction = PanelDirection.Vertical
    private val filePath = when {
        SystemUtils.IS_OS_WINDOWS -> """C:\"""
        SystemUtils.IS_OS_LINUX -> SystemUtils.getUserDir().toString()
        SystemUtils.IS_OS_MAC -> SystemUtils.getUserDir().toString()
        else -> """C:\"""
    }

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.FolderOpen, name)
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        Column {
            FileTreeUI(File(filePath))
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
}
