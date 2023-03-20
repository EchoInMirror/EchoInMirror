@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.eimsound.daw.components.trees

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.eimsound.audioprocessor.data.midi.MidiTrack
import com.eimsound.audioprocessor.data.midi.toMidiTracks
import com.eimsound.audioprocessor.data.midi.toOneMidiTrack
import com.eimsound.daw.components.dragdrop.FileDraggable
import com.eimsound.daw.components.dragdrop.GlobalDraggable
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.midi.MidiSystem

private val logger by lazy { KotlinLogging.logger("com.eimsound.daw.components.trees.FileTree") }

private val fileIcon = Icons.Outlined.InsertDriveFile
private val midiFileIcon = Icons.Outlined.Piano
private val midiTrackIcon = Icons.Outlined.Audiotrack
val FileExtensionIcons = mapOf(
    "mid" to midiFileIcon,
    "wav" to Icons.Outlined.MusicNote,
    "mp3" to Icons.Outlined.MusicNote,
    "ogg" to Icons.Outlined.MusicNote,
    "flac" to Icons.Outlined.MusicNote,
    "aiff" to Icons.Outlined.MusicNote,
    "aif" to Icons.Outlined.MusicNote,
    "jpg" to Icons.Outlined.Image,
    "jpeg" to Icons.Outlined.Image,
    "png" to Icons.Outlined.Image,
    "gif" to Icons.Outlined.Image,
    "bmp" to Icons.Outlined.Image,
    "tiff" to Icons.Outlined.Image,
    "tif" to Icons.Outlined.Image,
    "svg" to Icons.Outlined.Image,
    "pdf" to Icons.Outlined.PictureAsPdf,
    "txt" to Icons.Outlined.TextFields,
    "md" to Icons.Outlined.TextFields,
    "html" to Icons.Outlined.TextFields,
    "htm" to Icons.Outlined.TextFields,
)

private val expandIconModifier = Modifier.size(16.dp)
private val iconModifier = Modifier.size(18.dp)

@Composable
fun TreeItem(
    text: String,
    key: Any,
    icon: ImageVector? = null,
    expanded: Boolean? = null,
    depth: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val treeState = LocalTreeState.current
    Box(Modifier.fillMaxWidth().let {
        if (treeState?.selectedNode == key) it.background(MaterialTheme.colorScheme.secondary.copy(0.16F)) else it
    }.clickable {
        treeState?.selectedNode = key
        treeState?.onClick?.invoke(key)
        onClick?.invoke()
    }) {
        Row(Modifier.padding(start = 8.dp * depth, top = 1.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            if (expanded != null) Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                if (expanded) "收起" else "展开",
                expandIconModifier
            ) else Spacer(expandIconModifier)
            if (icon != null) Icon(icon, text, iconModifier)
            Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun DictionaryNode(file: File, depth: Int = 0) {
    var expanded by remember { mutableStateOf(false) }
    val list = remember(file) {
        try {
            file.listFiles()!!.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        } catch (e: Exception) {
            logger.error(e) { "Failed to list files in $file" }
            null
        }
    }
    val isExpandable = !list.isNullOrEmpty()
    TreeItem(
        file.name,
        file,
        if (expanded) Icons.Filled.FolderOpen else Icons.Outlined.Folder,
        if (isExpandable) expanded else null,
        depth,
        if (isExpandable) {
            { expanded = !expanded }
        } else null
    )
    if (expanded) list!!.fastForEach { FileNode(it, depth + 1) }
}

@Composable
fun MidiNode(file: File, depth: Int) {
    var expanded by remember { mutableStateOf(false) }
    GlobalDraggable({
        withContext(Dispatchers.IO) { MidiSystem.getSequence(file) }.toOneMidiTrack()
    }) {
        TreeItem(file.name, file, midiFileIcon, expanded, depth) { expanded = !expanded }
    }

    if (expanded) {
        var midiTracks by remember { mutableStateOf<List<MidiTrack>?>(null) }
        LaunchedEffect(file) {
            midiTracks = withContext(Dispatchers.IO) {
                MidiSystem.getSequence(file)
            }.toMidiTracks()
        }
        midiTracks?.fastForEachIndexed { index, midiTrack ->
            GlobalDraggable({ midiTrack }) {
                TreeItem(
                    midiTrack.name ?: "轨道 $index",
                    midiTrack,
                    midiTrackIcon,
                    depth = depth + 1
                )
            }
        }
    }
}

@Composable
fun DefaultFileNode(
    file: File,
    icon: ImageVector = FileExtensionIcons.getOrDefault(file.extension, fileIcon),
    expanded: Boolean? = null,
    depth: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    FileDraggable(file) {
        TreeItem(file.name, file, icon, expanded, depth, onClick)
    }
}

@Composable
fun FileNode(file: File, depth: Int = 0) {
    if (file.isDirectory) DictionaryNode(file, depth)
    else when (file.extension) {
        "mid" -> MidiNode(file, depth)
        else -> DefaultFileNode(file, depth = depth)
    }
}

class TreeState {
    var selectedNode by mutableStateOf<Any?>(null)
    var onClick: ((Any) -> Unit)? = null
        internal set
}

val LocalTreeState = staticCompositionLocalOf<TreeState?> { null }

@Composable
fun Tree(
    modifier: Modifier = Modifier.fillMaxSize(),
    onClick: ((Any) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier) {
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()
        val treeState = remember { TreeState() }
        treeState.onClick = onClick
        CompositionLocalProvider(LocalTreeState provides treeState) {
            BoxWithConstraints {
                Column(
                    Modifier.horizontalScroll(horizontalScrollState).verticalScroll(verticalScrollState)
                        .width(IntrinsicSize.Max).widthIn(min = maxWidth), content = content
                )
            }
        }
        HorizontalScrollbar(rememberScrollbarAdapter(horizontalScrollState), Modifier.align(Alignment.BottomCenter))
        VerticalScrollbar(rememberScrollbarAdapter(verticalScrollState), Modifier.align(Alignment.CenterEnd))
    }
}
