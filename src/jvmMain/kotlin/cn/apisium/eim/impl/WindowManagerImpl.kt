package cn.apisium.eim.impl

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.WindowManager
import cn.apisium.eim.window.editor.Editor
import cn.apisium.eim.window.Mixer
import cn.apisium.eim.window.dialogs.QuickLoadDialog
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import java.lang.ref.WeakReference

class WindowManagerImpl: WindowManager {
    override val dialogs = mutableStateMapOf<@Composable () -> Unit, Boolean>()
    override val panels = mutableStateListOf(Mixer, Editor)
    override var mainWindow: WeakReference<ComposeWindow> = WeakReference(null)
    override var isDarkTheme by mutableStateOf(false)
    override var activePanel: Panel? = null

    init {
        dialogs[SettingsWindow] = false
        dialogs[QuickLoadDialog] = false
    }

    override fun registerPanel(panel: Panel) {
        panels.add(panel)
    }

    override fun unregisterPanel(panel: Panel) {
        panels.remove(panel)
    }

    @Composable
    fun Dialogs() {
        for ((dialog, visible) in dialogs) {
            if (visible) dialog()
        }
    }
}
