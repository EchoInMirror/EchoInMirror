package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.*
import java.awt.Dimension
import java.util.*

val settingsTabsLoader: ServiceLoader<SettingTab> by lazy { ServiceLoader.load(SettingTab::class.java) }
val settingsTabs: List<SettingTab> get() = mutableListOf(AudioSettings, ShortcutKeySettings, FileBrowserSettings).apply {
    addAll(settingsTabsLoader)
    add(AboutPanel)
}

private fun closeSettingWindow() {
    EchoInMirror.windowManager.dialogs[SettingsWindow] = false
}

private val floatingLayerProvider = FloatingLayerProvider()

val SettingsWindow: @Composable () -> Unit = @Composable {
    DialogWindow(::closeSettingWindow, title = "设置") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false
        CompositionLocalProvider(LocalFloatingLayerProvider.provides(floatingLayerProvider)) {
            Tabs(settingsTabs) {
                Row(Modifier.padding(14.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Filled()
                    it?.buttons()
                    Button(::closeSettingWindow) { Text("确认") }
                }
            }
        }
        floatingLayerProvider.FloatingLayers()
    }
}
