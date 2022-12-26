package cn.apisium.eim.impl

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.AbstractCommand
import cn.apisium.eim.api.Command
import cn.apisium.eim.api.CommandManager
import cn.apisium.eim.api.DeleteCommand
import cn.apisium.eim.window.dialogs.settings.SettingsWindow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)
class CommandManagerImpl: CommandManager {
    private val commands = mutableMapOf<String, Command>()
    private val commandHandlers = mutableMapOf<Command, MutableSet<() -> Unit>>()

    init {
        registerCommand(DeleteCommand)
        registerCommand(object: AbstractCommand("EIM:Open Settings", arrayOf(Key.CtrlLeft, Key.Comma)) {
            override fun execute() {
                EchoInMirror.windowManager.dialogs[SettingsWindow] = true
            }
        })

        registerCommand(object: AbstractCommand("EIM:Play or Pause", arrayOf(Key.Spacebar)) {
            override fun execute() {
                EchoInMirror.currentPosition.isPlaying = !EchoInMirror.currentPosition.isPlaying
            }
        })

        registerCommand(object: AbstractCommand("EIM:Undo", arrayOf(Key.CtrlLeft, Key.Z)) {
            override fun execute() {
                GlobalScope.launch { EchoInMirror.undoManager.undo() }
            }
        })

        registerCommand(object: AbstractCommand("EIM:Redo", arrayOf(Key.CtrlLeft, Key.Y)) {
            override fun execute() {
                GlobalScope.launch { EchoInMirror.undoManager.redo() }
            }
        })
    }

    override fun registerCommand(command: Command) {
        commands[command.keyBindings.map { it.keyCode }.joinToString(" ")] = command
        commandHandlers[command] = hashSetOf()
    }

    override fun registerCommandHandler(command: Command, handler: () -> Unit) {
        commandHandlers[command]!!.add(handler)
    }

    override fun executeCommand(command: String) {
        val cmd = commands[command] ?: return
        cmd.execute()
        commandHandlers[cmd]!!.forEach { it() }
    }
}
