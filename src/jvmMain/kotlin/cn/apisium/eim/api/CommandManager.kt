package cn.apisium.eim.api

import androidx.compose.ui.input.key.Key

interface Command {
    val name: String
    val displayName: String
    fun execute() {}
    val keyBindings: Array<Key>
    val activeWhen: Array<String>?
}

abstract class AbstractCommand(
    override val name: String,
    override val displayName: String,
    override val keyBindings: Array<Key>,
    override val activeWhen: Array<String>? = null
) : Command

interface CommandManager {
    val commands: Map<String, Command>
    fun registerCommand(command: Command)
    fun registerCommandHandler(command: Command, handler: () -> Unit)
    fun executeCommand(command: String)
}
