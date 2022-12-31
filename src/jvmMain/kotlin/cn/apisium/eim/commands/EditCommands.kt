@file:OptIn(ExperimentalComposeUiApi::class)

package cn.apisium.eim.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.AbstractCommand
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteCommand: AbstractCommand("EIM:Delete", arrayOf(Key.Delete))
object CopyCommand: AbstractCommand("EIM:Copy", arrayOf(Key.CtrlLeft, Key.C))
object CutCommand: AbstractCommand("EIM:Cut", arrayOf(Key.CtrlLeft, Key.X))
object PasteCommand: AbstractCommand("EIM:Paste", arrayOf(Key.CtrlLeft, Key.V))
object SelectAllCommand: AbstractCommand("EIM:Select All", arrayOf(Key.CtrlLeft, Key.A))

object SaveCommand : AbstractCommand("EIM:Save", arrayOf(Key.CtrlLeft, Key.S)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.bus?.save() }
    }
}

object UndoCommand: AbstractCommand("EIM:Undo", arrayOf(Key.CtrlLeft, Key.Z)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.undo() }
    }
}

object RedoCommand: AbstractCommand("EIM:Redo", arrayOf(Key.CtrlLeft, Key.Y)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.redo() }
    }
}
