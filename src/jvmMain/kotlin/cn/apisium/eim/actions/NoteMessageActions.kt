package cn.apisium.eim.actions

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ReversibleAction
import cn.apisium.eim.api.UndoableAction
import cn.apisium.eim.data.midi.NoteMessage
import kotlinx.coroutines.runBlocking
import androidx.compose.material.icons.Icons
import cn.apisium.eim.components.icons.PencilPlus
import cn.apisium.eim.components.icons.PencilMinus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import cn.apisium.eim.api.MidiClip
import cn.apisium.eim.api.TrackClip

fun TrackClip<MidiClip>.doNoteAmountAction(noteMessage: Collection<NoteMessage>, isDelete: Boolean = false) {
    runBlocking { EchoInMirror.undoManager.execute(NoteAmountAction(this@doNoteAmountAction,
        noteMessage.toSet(), isDelete)) }
}

fun TrackClip<MidiClip>.doNoteMessageEditAction(noteMessage: Array<NoteMessage>, deltaX: Int, deltaY: Int, deltaDuration: Int) {
    if (deltaX == 0 && deltaY == 0 && deltaDuration == 0) return
    runBlocking {
        EchoInMirror.undoManager.execute(
            NoteMessageEditAction(
                this@doNoteMessageEditAction,
                noteMessage,
                deltaX,
                deltaY,
                deltaDuration
            )
        )
    }
}

fun MidiClip.doNoteVelocityAction(noteMessage: Array<NoteMessage>, deltaVelocity: Int) {
    if (deltaVelocity == 0) return
    runBlocking { EchoInMirror.undoManager.execute(NoteVelocityAction(this@doNoteVelocityAction,
        noteMessage, deltaVelocity)) }
}

class NoteAmountAction(private val clip: TrackClip<MidiClip>, private val notes: Set<NoteMessage>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = (if (isDelete) "音符删除 (" else "音符添加 (") + notes.size + "个)"
    override val icon = if (isDelete) PencilMinus else PencilPlus
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            clip.clip.notes.addAll(notes)
            clip.clip.notes.sort()
        } else clip.clip.notes.removeAll(notes)
        clip.reset()
        clip.clip.notes.update()
        return true
    }
}

class NoteMessageEditAction(
    private val clip: TrackClip<MidiClip>, private val notes: Array<NoteMessage>,
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
        clip.clip.notes.sort()
        clip.reset()
        clip.clip.notes.update()
        return true
    }
}

class NoteVelocityAction(
    private val clip: MidiClip, private val notes: Array<NoteMessage>,
    private val deltaVelocity: Int
) : UndoableAction {
    private val oldVelocities = notes.map { it.velocity }.toIntArray()
    override val name = "音符力度编辑 (${notes.size}个)"
    override val icon = Icons.Default.Tune
    override suspend fun undo(): Boolean {
        notes.forEachIndexed { index, noteMessage -> noteMessage.velocity = oldVelocities[index] }
        clip.notes.update()
        return true
    }

    override suspend fun execute(): Boolean {
        notes.forEach { it.velocity = (it.velocity + deltaVelocity).coerceIn(0, 127) }
        clip.notes.update()
        return true
    }
}
