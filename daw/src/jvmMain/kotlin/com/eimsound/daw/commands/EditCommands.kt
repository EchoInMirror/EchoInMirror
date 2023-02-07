@file:OptIn(ExperimentalComposeUiApi::class)

package com.eimsound.daw.commands

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.utils.BasicEditor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteCommand : AbstractCommand("EIM:Delete", "删除", arrayOf(Key.Delete)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.delete()
    }
}
object CopyCommand : AbstractCommand("EIM:Copy", "复制", arrayOf(Key.CtrlLeft, Key.C)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.copy()
    }
}
object CutCommand : AbstractCommand("EIM:Cut", "剪切", arrayOf(Key.CtrlLeft, Key.X)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.cut()
    }
}
object PasteCommand : AbstractCommand("EIM:Paste", "粘贴", arrayOf(Key.CtrlLeft, Key.V)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.paste()
    }
}
object SelectAllCommand : AbstractCommand("EIM:Select All", "选择全部", arrayOf(Key.CtrlLeft, Key.A)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.selectAll()
    }
}

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
