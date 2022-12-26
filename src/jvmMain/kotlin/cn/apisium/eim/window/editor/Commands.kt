package cn.apisium.eim.window.editor

import androidx.compose.ui.text.AnnotatedString
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteAmountAction
import cn.apisium.eim.commands.*
import cn.apisium.eim.data.midi.NoteMessage
import cn.apisium.eim.data.midi.NoteMessageImpl
import cn.apisium.eim.data.midi.NoteMessageWithInfo
import cn.apisium.eim.utils.CLIPBOARD_MANAGER
import cn.apisium.eim.utils.OBJECT_MAPPER
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.math.roundToInt

object EditorCommands {
    fun delete() {
        if (selectedNotes.isEmpty()) return
        val track = EchoInMirror.selectedTrack ?: return
        track.doNoteAmountAction(selectedNotes, true)
        selectedNotes.clear()
    }
    fun copy() {
        if (selectedNotes.isEmpty()) return
        CLIPBOARD_MANAGER?.setText(AnnotatedString(
            OBJECT_MAPPER.writeValueAsString(NoteMessageWithInfo(EchoInMirror.currentPosition.ppq, selectedNotes.toSet()))
        ))
    }
    fun cut() {
        val track = EchoInMirror.selectedTrack ?: return
        copy()
        track.doNoteAmountAction(selectedNotes, true)
        selectedNotes.clear()
    }
    fun paste() {
        val track = EchoInMirror.selectedTrack ?: return
        val content = CLIPBOARD_MANAGER?.getText()?.text ?: return
        try {
            val data = ObjectMapper()
                .registerModule(kotlinModule())
                .registerModule(SimpleModule()
                    .addAbstractTypeMapping(NoteMessage::class.java, NoteMessageImpl::class.java))
                .readValue<NoteMessageWithInfo>(content)
            val scale = EchoInMirror.currentPosition.ppq.toDouble() / data.ppq
            data.notes.forEach {
                it.time = (it.time * scale).roundToInt()
                it.duration = (it.duration * scale).roundToInt()
            }
            track.doNoteAmountAction(data.notes)
            selectedNotes.clear()
            selectedNotes.addAll(data.notes)
        } catch (ignored: Throwable) { ignored.printStackTrace() }
    }
    fun selectAll() {
        val track = EchoInMirror.selectedTrack ?: return
        selectedNotes.clear()
        selectedNotes.addAll(track.notes)
    }
}

internal fun registerCommandHandlers() {
    EchoInMirror.commandManager.apply {
        registerCommandHandler(DeleteCommand) {
            if (EchoInMirror.windowManager.activePanel == Editor) EditorCommands.delete()
        }
        registerCommandHandler(CopyCommand) {
            if (EchoInMirror.windowManager.activePanel == Editor) EditorCommands.copy()
        }
        registerCommandHandler(CutCommand) {
            if (EchoInMirror.windowManager.activePanel == Editor) EditorCommands.cut()
        }
        registerCommandHandler(PasteCommand) {
            if (EchoInMirror.windowManager.activePanel == Editor) EditorCommands.paste()
        }
        registerCommandHandler(SelectAllCommand) {
            if (EchoInMirror.windowManager.activePanel == Editor) EditorCommands.selectAll()
        }
    }
}