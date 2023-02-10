package com.eimsound.daw.window.panels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import cafe.adriel.bonsai.core.node.Node
import cafe.adriel.bonsai.filesystem.FileSystemTree
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.FileSystemStyle
import com.eimsound.daw.components.Tree
import com.eimsound.daw.components.dragdrop.FileDraggable
import okio.FileSystem
import okio.Path
import java.io.File

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

    @Composable
    override fun Content() {
        Tree(FileSystemTree(File("C:\\Python311"), true), FileSystemStyle, FileMapper)
    }
}
