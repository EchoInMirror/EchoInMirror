package cn.apisium.eim.impl

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.window.Editor
import cn.apisium.eim.window.Mixer
import cn.apisium.eim.window.settings.settingsWindow
import java.lang.ref.WeakReference

class WindowManagerImpl: WindowManager {
    override var settingsDialogOpen by mutableStateOf(false)
    override val panels = mutableStateListOf(Mixer(), Editor())
    override var mainWindow: WeakReference<ComposeWindow> = WeakReference(null)

    override fun registerPanel(panel: Panel) {
        panels.add(panel)
    }

    override fun unregisterPanel(panel: Panel) {
        panels.remove(panel)
    }

    @Composable
    fun dialogs() {
        if (settingsDialogOpen) settingsWindow()
    }
}
