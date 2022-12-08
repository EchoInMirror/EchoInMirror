package cn.apisium.eim.impl

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Command
import cn.apisium.eim.api.CommandManager

@OptIn(ExperimentalComposeUiApi::class)
class CommandManagerImpl: CommandManager {
    val commands = mutableMapOf<String, Command>()

    init {
        registerCommand(object: Command {
            override val name: String = "EIM:Open Settings"
            override fun execute() {
                EchoInMirror.windowManager.settingsDialogOpen = true
            }
            override val keyBindings = arrayOf(Key.CtrlLeft, Key.Comma)
            override val activeWhen: Array<String>? = null
        })

        registerCommand(object: Command {
            override val name: String = "EIM:Play or Pause"
            override fun execute() {
                EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying
            }
            override val keyBindings = arrayOf(Key.Spacebar)
            override val activeWhen: Array<String>? = null
        })
    }

    override fun registerCommand(command: Command) {
        commands[command.keyBindings.map { it.keyCode }.joinToString(" ")] = command
    }
}
