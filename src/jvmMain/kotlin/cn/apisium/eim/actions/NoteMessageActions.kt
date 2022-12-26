package cn.apisium.eim.actions

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ReversibleAction
import cn.apisium.eim.api.Track
import cn.apisium.eim.data.midi.NoteMessage
import kotlinx.coroutines.runBlocking

fun Track.doNoteAmountAction(noteMessage: Array<NoteMessage>, isDelete: Boolean) {
    val track = this
    runBlocking { EchoInMirror.undoManager.execute(NoteAmountAction(track, noteMessage, isDelete)) }
}

fun Track.doNoteMessageEditAction(noteMessage: Array<NoteMessage>, deltaX: Int, deltaY: Int, deltaDuration: Int) {
    val track = this
    runBlocking { EchoInMirror.undoManager.execute(NoteMessageEditAction(track, noteMessage, deltaX, deltaY, deltaDuration)) }
}

class NoteAmountAction(private val track: Track, private val notes: Array<NoteMessage>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = if (isDelete) "音符删除" else "音符添加"
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            track.notes.addAll(notes)
            track.notes.sort()
            track.onSuddenChange()
        } else track.notes.removeAll(notes.toSet())
        track.notes.update()
        return true
    }
}

class NoteMessageEditAction(private val track: Track, private val notes: Array<NoteMessage>,
                            private val deltaX: Int, private val deltaY: Int,
                            private val deltaDuration: Int) : ReversibleAction() {
    override val name = "音符编辑"
    override suspend fun perform(isForward: Boolean): Boolean {
        val x = if (isForward) deltaX else -deltaX
        val y = if (isForward) deltaY else -deltaY
        val duration = if (isForward) deltaDuration else -deltaDuration
        notes.forEach {
            it.time += x
            it.note += y
            it.duration += duration
        }
        track.notes.sort()
        track.onSuddenChange()
        track.notes.update()
        return true
    }
}
