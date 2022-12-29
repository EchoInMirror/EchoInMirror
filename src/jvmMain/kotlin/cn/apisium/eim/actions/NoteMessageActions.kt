package cn.apisium.eim.actions

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ReversibleAction
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.UndoableAction
import cn.apisium.eim.data.midi.NoteMessage
import kotlinx.coroutines.runBlocking
import androidx.compose.material.icons.Icons
import cn.apisium.eim.components.icons.PencilPlus
import cn.apisium.eim.components.icons.PencilMinus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune

fun Track.doNoteAmountAction(noteMessage: Collection<NoteMessage>, isDelete: Boolean = false) {
    val track = this
    runBlocking { EchoInMirror.undoManager.execute(NoteAmountAction(track, noteMessage.toSet(), isDelete)) }
}

fun Track.doNoteMessageEditAction(noteMessage: Array<NoteMessage>, deltaX: Int, deltaY: Int, deltaDuration: Int) {
    if (deltaX == 0 && deltaY == 0 && deltaDuration == 0) return
    val track = this
    runBlocking {
        EchoInMirror.undoManager.execute(
            NoteMessageEditAction(
                track,
                noteMessage,
                deltaX,
                deltaY,
                deltaDuration
            )
        )
    }
}

fun Track.doNoteVelocityAction(noteMessage: Array<NoteMessage>, deltaVelocity: Int) {
    if (deltaVelocity == 0) return
    val track = this
    runBlocking { EchoInMirror.undoManager.execute(NoteVelocityAction(track, noteMessage, deltaVelocity)) }
}

class NoteAmountAction(private val track: Track, private val notes: Set<NoteMessage>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = (if (isDelete) "音符删除 (" else "音符添加 (") + notes.size + "个)"
    override val icon = if (isDelete) PencilMinus else PencilPlus
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            track.notes.addAll(notes)
            track.notes.sort()
            track.onSuddenChange()
        } else track.notes.removeAll(notes)
        track.notes.update()
        return true
    }
}

class NoteMessageEditAction(
    private val track: Track, private val notes: Array<NoteMessage>,
    private val deltaX: Int, private val deltaY: Int,
    private val deltaDuration: Int
) : ReversibleAction() {
    override val name = "音符编辑 (${notes.size}个)"
    override val icon = Icons.Default.Edit
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

class NoteVelocityAction(
    private val track: Track, private val notes: Array<NoteMessage>,
    private val deltaVelocity: Int
) : UndoableAction {
    private val oldVelocities = notes.map { it.velocity }.toIntArray()
    override val name = "音符力度编辑 (${notes.size}个)"
    override val icon = Icons.Default.Tune
    override suspend fun undo(): Boolean {
        notes.forEachIndexed { index, noteMessage -> noteMessage.velocity = oldVelocities[index] }
        track.notes.update()
        return true
    }

    override suspend fun execute(): Boolean {
        notes.forEach { it.velocity = (it.velocity + deltaVelocity).coerceIn(0, 127) }
        track.notes.update()
        return true
    }
}
