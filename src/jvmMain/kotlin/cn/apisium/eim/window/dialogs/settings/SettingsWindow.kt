package cn.apisium.eim.window.dialogs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.Filled
import cn.apisium.eim.components.Tab
import cn.apisium.eim.components.Tabs
import java.awt.Dimension

private object AudioSettings: Tab {
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

val settingsTabs = mutableStateListOf(NativeAudioPluginSettings, AudioSettings, AboutPanel)
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
