@file:OptIn(ExperimentalComposeUiApi::class)

package com.eimsound.daw.commands

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.utils.BasicEditor
import com.eimsound.daw.dawutils.CLIPBOARD_MANAGER
import com.eimsound.daw.utils.SerializableEditor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteCommand : AbstractCommand("EIM:Delete", "删除", arrayOf(Key.Delete), Icons.Filled.DeleteForever) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.delete()
    }
}
object CopyCommand : AbstractCommand("EIM:Copy", "复制", arrayOf(Key.CtrlLeft, Key.C), Icons.Filled.ContentCopy) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.copy()
    }
}
object CopyToClipboard : AbstractCommand("EIM:CopyToClipboard", "复制到剪辑版", arrayOf(Key.CtrlLeft, Key.ShiftLeft, Key.C)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is SerializableEditor) CLIPBOARD_MANAGER?.setText(AnnotatedString(panel.copyAsString()))
    }
}
object CutCommand : AbstractCommand("EIM:Cut", "剪切", arrayOf(Key.CtrlLeft, Key.X), Icons.Filled.ContentCut) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.cut()
    }
}
object PasteCommand : AbstractCommand("EIM:Paste", "粘贴", arrayOf(Key.CtrlLeft, Key.V), Icons.Filled.ContentPaste) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.paste()
    }
}
object PasteFromClipboard : AbstractCommand("EIM:PasteFromClipboard", "从剪辑版粘贴", arrayOf(Key.CtrlLeft, Key.ShiftLeft, Key.V)) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is SerializableEditor) panel.pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return)
    }
}
object SelectAllCommand : AbstractCommand("EIM:Select All", "选择全部", arrayOf(Key.CtrlLeft, Key.A), Icons.Filled.SelectAll) {
    override fun execute() {
        super.execute()
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.selectAll()
    }
}

object SaveCommand : AbstractCommand("EIM:Save", "保存", arrayOf(Key.CtrlLeft, Key.S), Icons.Filled.Save) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.bus?.save() }
    }
}

object UndoCommand : AbstractCommand("EIM:Undo", "撤销", arrayOf(Key.CtrlLeft, Key.Z), Icons.Filled.Undo) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.undo() }
    }
}

object RedoCommand : AbstractCommand("EIM:Redo", "重做", arrayOf(Key.CtrlLeft, Key.Y), Icons.Filled.Redo) {
    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.redo() }
    }
}
