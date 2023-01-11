@file:OptIn(ExperimentalComposeUiApi::class)

package cn.apisium.eim.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.AbstractCommand
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteCommand : AbstractCommand("EIM:Delete", "删除", arrayOf(Key.Delete))
object CopyCommand : AbstractCommand("EIM:Copy", "复制", arrayOf(Key.CtrlLeft, Key.C))
object CutCommand : AbstractCommand("EIM:Cut", "剪切", arrayOf(Key.CtrlLeft, Key.X))
object PasteCommand : AbstractCommand("EIM:Paste", "粘贴", arrayOf(Key.CtrlLeft, Key.V))
object SelectAllCommand : AbstractCommand("EIM:Select All", "选择全部", arrayOf(Key.CtrlLeft, Key.A))

object SaveCommand : AbstractCommand("EIM:Save", "保存", arrayOf(Key.CtrlLeft, Key.S)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.bus?.save() }
    }
}

object UndoCommand : AbstractCommand("EIM:Undo", "撤销", arrayOf(Key.CtrlLeft, Key.Z)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.undo() }
    }
}

object RedoCommand : AbstractCommand("EIM:Redo", "重做", arrayOf(Key.CtrlLeft, Key.Y)) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.redo() }
    }
}
