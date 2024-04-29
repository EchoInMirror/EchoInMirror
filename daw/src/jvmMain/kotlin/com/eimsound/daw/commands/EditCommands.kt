package com.eimsound.daw.commands

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.api.CommandManager
import com.eimsound.daw.commons.BasicEditor
import com.eimsound.daw.dawutils.CLIPBOARD_MANAGER
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.commons.SerializableEditor
import com.eimsound.daw.language.langs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object DeleteCommand : AbstractCommand("EIM:Delete", arrayOf(Key.Delete), Icons.Filled.DeleteForever) {
    override val displayName get() = langs.delete

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor && panel.canDelete) panel.delete()
    }
}
object CopyCommand : AbstractCommand("EIM:Copy", arrayOf(Key.CtrlLeft, Key.C), Icons.Filled.ContentCopy) {
    override val displayName get() = langs.copy

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.copy()
    }
}
object CopyToClipboard : AbstractCommand("EIM:CopyToClipboard", arrayOf(Key.CtrlLeft, Key.ShiftLeft, Key.C)) {
    override val displayName get() = langs.copyToClipboard

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is SerializableEditor) CLIPBOARD_MANAGER?.setText(AnnotatedString(panel.copyAsString()))
    }
}
object CutCommand : AbstractCommand("EIM:Cut", arrayOf(Key.CtrlLeft, Key.X), Icons.Filled.ContentCut) {
    override val displayName get() = langs.cut

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.cut()
    }
}
object PasteCommand : AbstractCommand("EIM:Paste", arrayOf(Key.CtrlLeft, Key.V), Icons.Filled.ContentPaste) {
    override val displayName get() = langs.paste

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.paste()
    }
}
object PasteFromClipboard : AbstractCommand("EIM:PasteFromClipboard", arrayOf(Key.CtrlLeft, Key.ShiftLeft, Key.V)) {
    override val displayName get() = langs.pasteFromClipboard

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is SerializableEditor) panel.pasteFromString(CLIPBOARD_MANAGER?.getText()?.text ?: return)
    }
}
object SelectAllCommand : AbstractCommand("EIM:Select All", arrayOf(Key.CtrlLeft, Key.A), Icons.Filled.SelectAll) {
    override val displayName get() = langs.selectAll

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is MultiSelectableEditor) panel.selectAll()
    }
}

object SaveCommand : AbstractCommand("EIM:Save", arrayOf(Key.CtrlLeft, Key.S), Icons.Filled.Save) {
    override val displayName get() = langs.save

    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.bus?.save() }
    }
}

object UndoCommand : AbstractCommand("EIM:Undo", arrayOf(Key.CtrlLeft, Key.Z), Icons.Filled.Undo) {
    override val displayName get() = langs.undo

    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.undo() }
    }
}

object RedoCommand : AbstractCommand("EIM:Redo", arrayOf(Key.CtrlLeft, Key.Y), Icons.Filled.Redo) {
    override val displayName get() = langs.redo

    @OptIn(DelicateCoroutinesApi::class)
    override fun execute() {
        GlobalScope.launch { EchoInMirror.undoManager.redo() }
    }
}

object DuplicateCommand : AbstractCommand("EIM:Duplicate", arrayOf(Key.CtrlLeft, Key.D), Icons.Filled.ContentCopy) {
    override val displayName get() = langs.duplicate

    override fun execute() {
        val panel = EchoInMirror.windowManager.activePanel
        if (panel is BasicEditor) panel.duplicate()
    }
}

fun CommandManager.registerAllEditCommands() {
    registerCommand(DeleteCommand)
    registerCommand(CopyCommand)
    registerCommand(CopyToClipboard)
    registerCommand(CutCommand)
    registerCommand(PasteCommand)
    registerCommand(PasteFromClipboard)
    registerCommand(SelectAllCommand)
    registerCommand(SaveCommand)
    registerCommand(UndoCommand)
    registerCommand(RedoCommand)
    registerCommand(DuplicateCommand)
}
