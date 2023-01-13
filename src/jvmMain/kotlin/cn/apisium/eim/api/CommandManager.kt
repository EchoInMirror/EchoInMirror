package cn.apisium.eim.api

import androidx.compose.ui.ExperimentalComposeUiApi
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

@OptIn(ExperimentalComposeUiApi::class)
fun Collection<Key>.sortedKeys(): List<Key> {
    var hasCtrl = false
    var hasShift = false
    var hasAlt = false
    var hasMeta = false
    val list = distinct().filter {
        when (it) {
            Key.CtrlLeft -> hasCtrl = true
            Key.ShiftLeft -> hasShift = true
            Key.AltLeft -> hasAlt = true
            Key.MetaLeft -> hasMeta = true
            else -> return@filter true
        }
        return@filter false
    }
    val ret = mutableListOf<Key>()
    if (hasCtrl) ret.add(Key.CtrlLeft)
    if (hasShift) ret.add(Key.ShiftLeft)
    if (hasAlt) ret.add(Key.AltLeft)
    if (hasMeta) ret.add(Key.MetaLeft)
    ret.addAll(list.sortedBy { it.keyCode })
    return ret
}

@OptIn(ExperimentalComposeUiApi::class)
fun Collection<String>.sortedStrKeys(): List<String> {
    var hasCtrl = false
    var hasShift = false
    var hasAlt = false
    var hasMeta = false
    val list = distinct().filter {
        when (it) {
            Key.CtrlLeft.keyCode.toString() -> hasCtrl = true
            Key.ShiftLeft.keyCode.toString() -> hasShift = true
            Key.AltLeft.keyCode.toString() -> hasAlt = true
            Key.MetaLeft.keyCode.toString() -> hasMeta = true
            else -> return@filter true
        }
        return@filter false
    }
    val ret = mutableListOf<String>()
    if (hasCtrl) ret.add(Key.CtrlLeft.keyCode.toString())
    if (hasShift) ret.add(Key.ShiftLeft.keyCode.toString())
    if (hasAlt) ret.add(Key.AltLeft.keyCode.toString())
    if (hasMeta) ret.add(Key.MetaLeft.keyCode.toString())
    ret.addAll(list.sortedBy { it })
    return ret
}

fun Array<Key>.sortedKeys() = toList().sortedKeys()
fun Collection<Key>.getKeys() = sortedKeys().joinToString(" ") { it.keyCode.toString() }
fun Array<Key>.getKeys() = sortedKeys().getKeys()
