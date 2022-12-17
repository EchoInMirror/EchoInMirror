package cn.apisium.eim.api.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import java.lang.ref.WeakReference

interface WindowManager {
    val dialogs: MutableMap<@Composable () -> Unit, Boolean>
    val panels: List<Panel>
    val mainWindow: WeakReference<ComposeWindow>

    fun registerPanel(panel: Panel)
    fun unregisterPanel(panel: Panel)
}
