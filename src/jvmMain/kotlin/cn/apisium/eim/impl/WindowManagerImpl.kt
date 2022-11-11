package cn.apisium.eim.impl

import androidx.compose.runtime.*
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.utils.mutableStateSetOf
import cn.apisium.eim.window.Editor
import cn.apisium.eim.window.Mixer
import cn.apisium.eim.window.settings.settingsWindow

class WindowManagerImpl: WindowManager {
    override var settingsDialogOpen by mutableStateOf(false)
    override val panels = mutableStateSetOf(Mixer(), Editor())

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
