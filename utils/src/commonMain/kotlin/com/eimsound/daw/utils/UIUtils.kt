package com.eimsound.daw.utils

import androidx.compose.runtime.Composable
import org.apache.commons.lang3.SystemUtils
import java.awt.Component
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser

@Suppress("INVISIBLE_MEMBER")
val CurrentWindow @Composable get() = androidx.compose.ui.window.LocalWindow

fun openFolderBrowser(parent: Component? = null): File? {
    val fileChooser = JFileChooser()
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showOpenDialog(parent)
    return fileChooser.selectedFile
}

fun openInExplorer(file: File) = Desktop.getDesktop().open(file)
fun openInBrowser(uri: URI) = Desktop.getDesktop().browse(uri)
fun selectInExplorer(file: File) {
    Desktop.getDesktop().apply {
        if (isSupported(Desktop.Action.BROWSE_FILE_DIR)) browseFileDirectory(file)
        else if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,\"${file.absolutePath}\""))
    }
}