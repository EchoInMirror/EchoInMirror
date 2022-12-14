@file:OptIn(ExperimentalComposeUiApi::class)

package cn.apisium.eim.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.AbstractCommand
import cn.apisium.eim.window.dialogs.settings.SettingsWindow

object OpenSettingsCommand: AbstractCommand("EIM:Open Settings", arrayOf(Key.CtrlLeft, Key.Comma)) {
    override fun execute() {
        EchoInMirror.windowManager.dialogs[SettingsWindow] = true
    }
}

object PlayOrPauseCommand: AbstractCommand("EIM:Play or Pause", arrayOf(Key.Spacebar)) {
    override fun execute() {
        EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying
    }
}
