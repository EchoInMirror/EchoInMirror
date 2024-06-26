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
import com.eimsound.dsp.data.midi.MidiTrack
import com.eimsound.dsp.data.midi.toMidiTracks
import com.eimsound.daw.api.FileExtensionManager
import com.eimsound.daw.components.dragdrop.FileDraggable
import com.eimsound.daw.components.dragdrop.GlobalDraggable
import com.eimsound.daw.language.langs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.midi.MidiSystem
import kotlin.io.path.*

private val fileIcon = Icons.Outlined.InsertDriveFile
private val midiFileIcon = Icons.Outlined.Piano
private val midiTrackIcon = Icons.Outlined.Audiotrack
val FileExtensionIcons = mapOf(
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
                if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                if (expanded) langs.collapse else langs.expand,
                expandIconModifier
            ) else Spacer(expandIconModifier)
            if (icon != null) Icon(icon, text, iconModifier)
            Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun DictionaryNode(file: Path, depth: Int = 0, showSupFormatOnly: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    var list: List<Path>? by remember { mutableStateOf(null) }
    val isExpandable = !list.isNullOrEmpty()
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            list = try {
                file.listDirectoryEntries()
                    .filter { !it.isHidden() }
                    .sortedWith(compareBy({ !it.isDirectory() }, { it.name }))
            } catch (ignored: Throwable) {
                null
            }
        }
    }
    TreeItem(
        file.name.ifEmpty {
            if (depth == 0) {
                val str = file.pathString
                if (str == "/") langs.rootPath else str
            } else langs.untitled
        },
        file,
        if (expanded) Icons.Filled.FolderOpen else Icons.Outlined.Folder,
        if (isExpandable) expanded else null,
        depth,
        if (isExpandable) {
            { expanded = !expanded }
        } else null
    )
    if (expanded) list?.fastForEach { key(it) { FileNode(it, depth + 1, showSupFormatOnly) } }
}

@Composable
fun MidiNode(file: Path, depth: Int) {
    var expanded by remember { mutableStateOf(false) }
    GlobalDraggable({
        file
    }) {
        TreeItem(file.name, file, midiFileIcon, expanded, depth) { expanded = !expanded }
    }

    if (expanded) {
        var midiTracks by remember { mutableStateOf<List<MidiTrack>?>(null) }
        LaunchedEffect(file) {
            midiTracks = withContext(Dispatchers.IO) {
                MidiSystem.getSequence(Files.newInputStream(file))
            }.toMidiTracks()
        }
        midiTracks?.fastForEachIndexed { index, midiTrack ->
            GlobalDraggable({ file to index }) {
                TreeItem(
                    midiTrack.name ?: "${langs.track} $index",
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
    file: Path,
    icon: ImageVector = FileExtensionManager.getHandler(file)?.icon
        ?: FileExtensionIcons.getOrDefault(file.extension.lowercase(), fileIcon),
    expanded: Boolean? = null,
    depth: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    FileDraggable(file) {
        TreeItem(file.name, file, icon, expanded, depth, onClick)
    }
}

@Composable
fun FileNode(file: Path, depth: Int = 0, showSupFormatOnly: Boolean = false) {
    if (file.isDirectory()) DictionaryNode(file, depth, showSupFormatOnly)
    else {
        val ext = FileExtensionManager.handlers
            .firstOrNull { it.extensions.containsMatchIn(file.name) }
        if (ext?.isCustomFileBrowserNode == true) ext.FileBrowserNode(file, depth)
        else if (!showSupFormatOnly || ext != null) DefaultFileNode(file, depth = depth)
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
