package com.eimsound.daw.commands

import androidx.compose.ui.input.key.Key
import com.eimsound.daw.api.AbstractCommand
import com.eimsound.daw.api.CommandManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.EditorTool

object CursorToolCommand : AbstractCommand("EIM:CursorTool", "光标工具", arrayOf(Key.One)) {
    override fun execute() {
        EchoInMirror.editorTool = EditorTool.CURSOR
    }
}

object PencilToolCommand : AbstractCommand("EIM:PencilTool", "铅笔工具", arrayOf(Key.Two)) {
    override fun execute() {
        EchoInMirror.editorTool = EditorTool.PENCIL
    }
}

object EraserToolCommand : AbstractCommand("EIM:EraserTool", "橡皮擦工具", arrayOf(Key.Three)) {
    override fun execute() {
        EchoInMirror.editorTool = EditorTool.ERASER
    }
}

object MuteToolCommand : AbstractCommand("EIM:MuteTool", "静音工具", arrayOf(Key.Four)) {
    override fun execute() {
        EchoInMirror.editorTool = EditorTool.MUTE
    }
}

object CutToolCommand : AbstractCommand("EIM:CutTool", "刀片工具", arrayOf(Key.Five)) {
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
