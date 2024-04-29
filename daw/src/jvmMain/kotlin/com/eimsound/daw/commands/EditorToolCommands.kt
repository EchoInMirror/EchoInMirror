package com.eimsound.daw.commands

import androidx.compose.ui.input.key.Key
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.api.CommandManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.EditorTool
import com.eimsound.daw.language.langs

object CursorToolCommand : AbstractCommand("EIM:CursorTool", arrayOf(Key.One)) {
    override val displayName get() = langs.editorToolsLangs.cursor

    override fun execute() {
        EchoInMirror.editorTool = EditorTool.CURSOR
    }
}

object PencilToolCommand : AbstractCommand("EIM:PencilTool", arrayOf(Key.Two)) {
    override val displayName get() = langs.editorToolsLangs.pencil

    override fun execute() {
        EchoInMirror.editorTool = EditorTool.PENCIL
    }
}

object EraserToolCommand : AbstractCommand("EIM:EraserTool", arrayOf(Key.Three)) {
    override val displayName get() = langs.editorToolsLangs.eraser

    override fun execute() {
        EchoInMirror.editorTool = EditorTool.ERASER
    }
}

object MuteToolCommand : AbstractCommand("EIM:MuteTool", arrayOf(Key.Four)) {
    override val displayName get() = langs.editorToolsLangs.mute

    override fun execute() {
        EchoInMirror.editorTool = EditorTool.MUTE
    }
}

object CutToolCommand : AbstractCommand("EIM:CutTool", arrayOf(Key.Five)) {
    override val displayName get() = langs.editorToolsLangs.cut

    override fun execute() {
        EchoInMirror.editorTool = EditorTool.CUT
    }
}

fun CommandManager.registerAllEditorToolCommands() {
    registerCommand(CursorToolCommand)
    registerCommand(PencilToolCommand)
    registerCommand(EraserToolCommand)
    registerCommand(MuteToolCommand)
    registerCommand(CutToolCommand)
}
