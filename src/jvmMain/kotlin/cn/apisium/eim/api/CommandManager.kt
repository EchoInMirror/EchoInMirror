package cn.apisium.eim.api

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key

interface Command {
    val name: String
    fun execute() { }
    val keyBindings: Array<Key>
    val activeWhen: Array<String>?
}

abstract class AbstractCommand(override val name: String, override val keyBindings: Array<Key>, override val activeWhen: Array<String>? = null): Command

@OptIn(ExperimentalComposeUiApi::class)
object DeleteCommand: AbstractCommand("EIM:Delete", arrayOf(Key.Delete))

interface CommandManager {
    fun registerCommand(command: Command)
    fun registerCommandHandler(command: Command, handler: () -> Unit)
    fun executeCommand(command: String)
}
