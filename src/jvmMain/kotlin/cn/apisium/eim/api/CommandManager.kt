package cn.apisium.eim.api

import androidx.compose.ui.input.key.Key

interface Command {
    val name: String
    fun execute()
    val keyBindings: Array<Key>
    val activeWhen: Array<String>?
}

interface CommandManager {
    fun registerCommand(command: Command)
}
