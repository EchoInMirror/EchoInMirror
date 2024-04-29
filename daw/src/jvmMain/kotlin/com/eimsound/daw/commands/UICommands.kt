package com.eimsound.daw.commands

import androidx.compose.ui.input.key.Key
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.api.CommandManager
import com.eimsound.daw.impl.WindowManagerImpl
import com.eimsound.daw.language.langs
import com.eimsound.daw.window.dialogs.openQuickLoadDialog
import com.eimsound.daw.window.dialogs.settings.SettingsWindow

object OpenSettingsCommand : AbstractCommand("EIM:Open Settings", arrayOf(Key.CtrlLeft, Key.Comma)) {
    override val displayName get() = langs.openSetting

    override fun execute() {
        EchoInMirror.windowManager.dialogs[SettingsWindow] = true
    }
}

object OpenQuickLoadDialogCommand : AbstractCommand("EIM:Open Quick Load Dialog", arrayOf(Key.Grave)) {
    override val displayName get() = langs.openQuickLoad

    override fun execute() {
        (EchoInMirror.windowManager as WindowManagerImpl).floatingLayerProvider?.openQuickLoadDialog()
    }
}

object PlayOrPauseCommand : AbstractCommand("EIM:Play or Pause", arrayOf(Key.Spacebar)) {
    override val displayName get() = langs.pausePlay

    override fun execute() {
        EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying
    }
}

fun CommandManager.registerAllUICommands() {
    registerCommand(OpenSettingsCommand)
    registerCommand(OpenQuickLoadDialogCommand)
    registerCommand(PlayOrPauseCommand)
}
