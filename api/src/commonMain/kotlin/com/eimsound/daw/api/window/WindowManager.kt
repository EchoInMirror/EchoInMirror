package com.eimsound.daw.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.awt.ComposeWindow
import java.nio.file.Path
import java.util.*

data class GlobalException(val exception: Throwable, val id: UUID)

/**
 * @see com.eimsound.daw.impl.WindowManagerImpl
 */
interface WindowManager {
    val dialogs: SnapshotStateMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: ComposeWindow?
    var isDarkTheme: Boolean
    var activePanel: Panel?
    val isMainWindowOpened: Boolean
    var isSaveProjectWarningDialogOpened: Boolean
    var globalException: GlobalException?

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
    fun openProject(path: Path)
    fun closeMainWindow(force: Boolean = false)
    fun exitApplication()
}
