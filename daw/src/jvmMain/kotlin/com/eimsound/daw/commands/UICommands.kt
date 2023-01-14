@file:OptIn(ExperimentalComposeUiApi::class)

package com.eimsound.daw.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.window.dialogs.settings.SettingsWindow

object OpenSettingsCommand : AbstractCommand("EIM:Open Settings", "打开设置", arrayOf(Key.CtrlLeft, Key.Comma)) {
    override fun execute() {
        EchoInMirror.windowManager.dialogs[SettingsWindow] = true
    }
}

object PlayOrPauseCommand : AbstractCommand("EIM:Play or Pause", "暂停/播放", arrayOf(Key.Spacebar)) {
    override fun execute() {
        EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying
    }
}
