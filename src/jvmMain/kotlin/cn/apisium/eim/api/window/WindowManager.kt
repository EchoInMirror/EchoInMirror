package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.awt.ComposeWindow
import java.lang.ref.WeakReference

interface WindowManager {
    val dialogs: SnapshotStateMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: WeakReference<ComposeWindow>
    var isDarkTheme: Boolean

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
}
