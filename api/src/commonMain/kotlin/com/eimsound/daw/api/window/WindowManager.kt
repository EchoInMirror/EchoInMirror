package com.eimsound.daw.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.awt.ComposeWindow
import com.eimsound.daw.api.processor.Track
import java.nio.file.Path

interface WindowManager {
    val dialogs: SnapshotStateMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: ComposeWindow?
    var isDarkTheme: Boolean
    var activePanel: Panel?
    val isMainWindowOpened: Boolean

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
    fun clearTrackUIState(track: Track)
    fun openProject(path: Path)
    fun closeMainWindow(isExit: Boolean = false)
}
