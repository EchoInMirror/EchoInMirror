package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import java.lang.ref.WeakReference

interface WindowManager {
    val dialogs: SnapshotStateMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: WeakReference<ComposeWindow>
    var isDarkTheme: Boolean
    var activePanel: Panel?

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
    fun openFloatingDialog(onClose: ((Any) -> Unit)? = null, position: Offset? = null, key: Any? = null,
                           hasOverlay: Boolean = false, content: @Composable () -> Unit): Any
    fun closeFloatingDialog(key: Any)
}
