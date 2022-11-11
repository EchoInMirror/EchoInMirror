package cn.apisium.eim.impl

import androidx.compose.runtime.*
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.window.settings.settingsWindow

class WindowManagerImpl: WindowManager {
    override var settingsDialogOpen by mutableStateOf(true)

    @Composable
    fun dialogs() {
        if (settingsDialogOpen) settingsWindow()
    }
}
