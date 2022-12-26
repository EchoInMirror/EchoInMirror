package cn.apisium.eim.actions

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.UndoableAction
import cn.apisium.eim.data.midi.NoteMessage
import kotlinx.coroutines.runBlocking

fun Track.doAddNoteMessage(noteMessage: Array<NoteMessage>, isDelete: Boolean) {
    val track = this
    runBlocking { EchoInMirror.undoManager.execute(NoteAmountAction(track, noteMessage, isDelete)) }
}

class NoteAmountAction(private val track: Track, private val notes: Array<NoteMessage>, private val isDelete: Boolean) :
    UndoableAction {
    override val name = "音符编辑"
    override suspend fun execute(): Boolean {
        perform(isDelete)
        return true
    }
    override suspend fun undo(): Boolean {
        perform(!isDelete)
        return true
    }
    private fun perform(isDelete: Boolean) {
        if (isDelete) track.notes.removeAll(notes.toSet()) else {
            track.notes.addAll(notes)
            track.notes.sort()
            track.onSuddenChange()
        }
        track.notes.update()
    }
}
