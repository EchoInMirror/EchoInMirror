package com.eimsound.daw.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.SystemUtils
import java.awt.Component
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import javax.swing.JFileChooser

@Suppress("INVISIBLE_MEMBER")
val CurrentWindow @Composable get() = androidx.compose.ui.window.LocalWindow

@OptIn(DelicateCoroutinesApi::class)
fun openFolderBrowser(parent: Component? = null, callback: (File?) -> Unit) {
    if (SystemUtils.IS_OS_MAC) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "/usr/bin/osascript",
                        "-e",
                        "set selectedFolder to choose folder with prompt \"Select Folder\"\nreturn POSIX path of selectedFolder"
                    )
                )
                val result = process.waitFor()
                if (result == 0) {
                    callback(File(BufferedReader(InputStreamReader(process.inputStream)).readLine()))
                    return@launch
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            callback(null)
        }
    } else JFileChooser().run {
        dialogTitle = "Select Folder"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        callback(if (showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) selectedFile else null)
    }
}

fun openInExplorer(file: File) {
    try {
        if (!file.exists()) return
        Desktop.getDesktop().open(file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun openInBrowser(uri: URI) {
    try {
        Desktop.getDesktop().browse(uri)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun selectInExplorer(file: File) {
    try {
        if (!file.exists()) return
        Desktop.getDesktop().apply {
            if (isSupported(Desktop.Action.BROWSE_FILE_DIR)) browseFileDirectory(file)
            else if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,\"${file.absolutePath}\""))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.clearFocus() = composed {
    val manager = LocalFocusManager.current
    onPointerEvent(PointerEventType.Press) { manager.clearFocus(true) }
}

inline fun <T : Any> Modifier.ifNotNull(value: T?, block: Modifier.(T) -> Modifier) = if (value == null) this else block(value)
