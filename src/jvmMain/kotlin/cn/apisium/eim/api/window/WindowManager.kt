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
    fun <T> openFloatingDialog(onClose: (() -> Unit)? = null, position: Offset? = null, key: T? = null,
                           hasOverlay: Boolean = false, content: @Composable () -> Unit): T
    fun closeFloatingDialog(key: Any)
}
