@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.eimsound.daw.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.bonsai.core.BonsaiScope
import cafe.adriel.bonsai.core.BonsaiStyle
import cafe.adriel.bonsai.core.OnNodeClick
import cafe.adriel.bonsai.core.node.BranchNode
import cafe.adriel.bonsai.core.node.Node
import cafe.adriel.bonsai.core.tree.Tree
import okio.Path
import kotlin.io.path.extension

val FileExtensionIcons = mapOf(
    "mid" to Icons.Outlined.Piano,
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
    "midiTrack" to Icons.Outlined.Audiotrack
)

val FileSystemStyle = BonsaiStyle<Path>(
    nodeNameStartPadding = 4.dp,
    nodeCollapsedIcon = { node ->
        rememberVectorPainter(
            if (node is BranchNode) Icons.Outlined.Folder
            else FileExtensionIcons[node.content.toNioPath().extension.lowercase()] ?: Icons.Outlined.InsertDriveFile
        )
    },
    nodeExpandedIcon = { rememberVectorPainter(Icons.Outlined.FolderOpen) }
)

@Composable
fun <T : Any> Tree(
    tree: Tree<T>,
    style: BonsaiStyle<T> = BonsaiStyle(),
    mapper: (@Composable (Node<T>, @Composable () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onLongClick: OnNodeClick<T> = { },
    onDoubleClick: OnNodeClick<T> = { },
    onClick: OnNodeClick<T> = { }
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val labelLarge = MaterialTheme.typography.labelLarge
    val style0 = remember(style, labelLarge, onSurfaceColor) {
        val tint = ColorFilter.tint(onSurfaceColor)
        style.copy(
            nodeNameTextStyle = labelLarge.copy(onSurfaceColor),
            nodeExpandedIconColorFilter = tint,
            nodeCollapsedIconColorFilter = tint,
            toggleIconColorFilter = tint,
        )
    }
    remember(tree, style0) {
        BonsaiScope(tree, tree, style0, onClick, onLongClick, onDoubleClick)
    }.apply {
        Box(modifier) {
            val horizontalScrollState = rememberScrollState()
            val state = rememberLazyListState()
            LazyColumn(Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState), state) {
                items(tree.nodes, { it.key }) { node ->
                    if (mapper == null) Node(node)
                    else mapper(node) { Node(node) }
                }
            }
            HorizontalScrollbar(rememberScrollbarAdapter(horizontalScrollState), Modifier.align(Alignment.BottomStart))
            VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd))
        }
    }
}
