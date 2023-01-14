package com.eimsound.daw.window.dialogs.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.Filled
import com.eimsound.daw.components.Tab
import com.eimsound.daw.components.Tabs
import java.awt.Dimension

private object AudioSettings : Tab {
    @Composable
    override fun label() {
        Text("音频")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.SettingsInputComponent, "Audio Settings")
    }

    @Composable
    override fun content() {
        BasicText("音频设置")
    }
}

val settingsTabs = mutableStateListOf(NativeAudioPluginSettings, AudioSettings, ShortcutKeySettings, AboutPanel)
private fun closeSettingWindow() {
    EchoInMirror.windowManager.dialogs[SettingsWindow] = false
}

val SettingsWindow: @Composable () -> Unit = @Composable {
    Dialog(::closeSettingWindow, title = "设置") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false
        Tabs(settingsTabs) {
            Row(Modifier.padding(14.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filled()
                it?.buttons()
                Button(::closeSettingWindow) { Text("确认") }
            }
        }
    }
}
