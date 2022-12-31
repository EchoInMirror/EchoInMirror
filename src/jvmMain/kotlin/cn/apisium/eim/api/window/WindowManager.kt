package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import cn.apisium.eim.api.processor.Track
import java.lang.ref.WeakReference
import java.nio.file.Path

interface WindowManager {
    val dialogs: SnapshotStateMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: WeakReference<ComposeWindow>
    var isDarkTheme: Boolean
    var activePanel: Panel?
    val isMainWindowOpened: Boolean

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
    fun openFloatingDialog(onClose: ((Any) -> Unit)? = null, position: Offset? = null, key: Any? = null,
                           hasOverlay: Boolean = false, content: @Composable () -> Unit): Any
    fun closeFloatingDialog(key: Any)
    fun clearTrackUIState(track: Track)
    fun openProject(path: Path)
    fun closeMainWindow()
}
