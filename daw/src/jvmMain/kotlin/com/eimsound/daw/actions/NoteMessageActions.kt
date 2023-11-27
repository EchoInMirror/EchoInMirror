package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.util.fastForEach
import com.eimsound.dsp.data.midi.NoteMessage
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.MidiClip
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.commons.actions.ListDisabledAction
import com.eimsound.daw.components.icons.PencilMinus
import com.eimsound.daw.components.icons.PencilPlus
import com.eimsound.daw.commons.actions.ReversibleAction
import com.eimsound.daw.commons.actions.UndoableAction
import kotlinx.coroutines.runBlocking

fun TrackClip<MidiClip>.doNoteAmountAction(noteMessage: Collection<NoteMessage>, isDelete: Boolean = false) {
    runBlocking { EchoInMirror.undoManager.execute(NoteAmountAction(this@doNoteAmountAction,
        noteMessage.toList(), isDelete)) }
}

fun TrackClip<MidiClip>.doNoteMessageEditAction(noteMessage: Collection<NoteMessage>, deltaX: Int, deltaY: Int, deltaDuration: Int) {
    if (deltaX == 0 && deltaY == 0 && deltaDuration == 0) return
    runBlocking {
        EchoInMirror.undoManager.execute(
            NoteMessageEditAction(
                this@doNoteMessageEditAction,
                noteMessage.toList(),
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

fun MidiClip.doNoteDisabledAction(noteMessage: List<NoteMessage>, isDisabled: Boolean? = null) {
    runBlocking { EchoInMirror.undoManager.execute(NotesDisabledAction(this@doNoteDisabledAction,
        noteMessage, isDisabled)) }
}

class NoteAmountAction(private val clip: TrackClip<MidiClip>, private val notes: Collection<NoteMessage>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = (if (isDelete) "音符删除 (" else "音符添加 (") + notes.size + "个)"
    override val icon = if (isDelete) PencilMinus else PencilPlus
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            clip.clip.notes.addAll(notes)
            clip.clip.notes.sort()
        } else clip.clip.notes.removeAll(notes.toSet())
        clip.reset()
        clip.clip.notes.update()
        return true
    }
}

class NoteMessageEditAction(
    private val clip: TrackClip<MidiClip>, private val notes: Collection<NoteMessage>,
    private val deltaX: Int, private val deltaY: Int,
    private val deltaDuration: Int
) : UndoableAction {
    private val oldNotes = notes.map { it.note }
    override val name = "音符编辑 (${notes.size}个)"
    override val icon = Icons.Default.Edit

    override suspend fun undo(): Boolean {
        notes.forEachIndexed { index, noteMessage ->
            noteMessage.time -= deltaX
            noteMessage.note = oldNotes[index]
            noteMessage.duration -= deltaDuration
        }
        clip.clip.notes.sort()
        clip.reset()
        clip.clip.notes.update()
        return true
    }

    override suspend fun execute(): Boolean {
        notes.forEach {
            it.time += deltaX
            it.note += deltaY
            it.duration += deltaDuration
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
    private val oldVelocities = notes.map { it.velocity }
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

class NotesDisabledAction(
    private val clip: MidiClip, notes: List<NoteMessage>, isDisabled: Boolean? = null
) : ListDisabledAction(notes, isDisabled) {
    override val name = if (isDisabled ?: (notes.firstOrNull()?.isDisabled == false)) "音符禁用 (${notes.size}个)" else "音符启用 (${notes.size}个)"
    override val icon = Icons.Default.Edit

    override fun afterPerform() { clip.notes.update() }
}

fun MidiClip.doNotesSplitAction(notes: List<NoteMessage>, clickTime: Int) {
    runBlocking { EchoInMirror.undoManager.execute(NotesSplitAction(this@doNotesSplitAction,
        notes, clickTime)) }
}

class NotesSplitAction(
    private val clip: MidiClip, private val notes: List<NoteMessage>, private val clickTime: Int
) : UndoableAction {
    private val newNotes = mutableListOf<NoteMessage>()
    private val modifiedNotes = mutableListOf<Pair<NoteMessage, Int>>()
    override val name = "音符分割 (${notes.size}个)"
    override val icon = Icons.Default.Edit

    override suspend fun undo(): Boolean {
        clip.notes.removeAll(newNotes)
        modifiedNotes.fastForEach { it.first.duration = it.second }
        clip.notes.update()
        return true
    }

    override suspend fun execute(): Boolean {
        newNotes.clear()
        notes.fastForEach {
            if (it.time + it.duration <= clickTime || it.time >= clickTime) return@fastForEach
            newNotes.add(it.copy(time = clickTime, duration = it.time + it.duration - clickTime))
            modifiedNotes.add(it to it.duration)
            it.duration = clickTime - it.time
        }
        clip.notes.addAll(newNotes)
        clip.notes.sort()
        clip.notes.update()
        return true
    }
}
