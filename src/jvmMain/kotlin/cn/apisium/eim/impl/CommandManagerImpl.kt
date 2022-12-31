package cn.apisium.eim.impl

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import cn.apisium.eim.api.Command
import cn.apisium.eim.api.CommandManager
import cn.apisium.eim.commands.*

@OptIn(ExperimentalComposeUiApi::class)
class CommandManagerImpl: CommandManager {
    private val commands = mutableMapOf<String, Command>()
    private val commandHandlers = mutableMapOf<Command, MutableSet<() -> Unit>>()

    init {
        registerCommand(DeleteCommand)
        registerCommand(CopyCommand)
        registerCommand(CutCommand)
        registerCommand(PasteCommand)
        registerCommand(SelectAllCommand)
        registerCommand(SaveCommand)

        registerCommand(OpenSettingsCommand)
        registerCommand(PlayOrPauseCommand)
        registerCommand(UndoCommand)
        registerCommand(RedoCommand)
    }

    override fun registerCommand(command: Command) {
        var hasCtrl = false
        var hasShift = false
        var hasAlt = false
        var hasMeta = false
        var keys = ""
        command.keyBindings.forEach {
            when (it) {
                Key.CtrlLeft -> hasCtrl = true
                Key.ShiftLeft -> hasShift = true
                Key.AltLeft -> hasAlt = true
                Key.MetaLeft -> hasMeta = true
                else -> keys += "${it.keyCode} "
            }
        }

        if (hasCtrl) keys = "${Key.CtrlLeft.keyCode} $keys"
        if (hasShift) keys = "${Key.ShiftLeft.keyCode} $keys"
        if (hasAlt) keys = "${Key.AltLeft.keyCode} $keys"
        if (hasMeta) keys = "${Key.MetaLeft.keyCode} $keys"
        commands[keys.trim()] = command
        commandHandlers[command] = hashSetOf()
    }

    override fun registerCommandHandler(command: Command, handler: () -> Unit) {
        commandHandlers[command]?.add(handler) ?: throw IllegalArgumentException("Command ${command.name} not registered")
    }

    override fun executeCommand(command: String) {
        val cmd = commands[command] ?: return
        cmd.execute()
        commandHandlers[cmd]!!.forEach { it() }
    }
}
