package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.eimsound.dsp.data.EnvelopePointList
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.MutableMidiCCEvents
import com.eimsound.daw.commons.actions.ReversibleAction
import kotlinx.coroutines.runBlocking

fun MutableMidiCCEvents.doAddOrRemoveMidiCCEventAction(id: Int, points: EnvelopePointList? = null) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            MidiCCEventAddOrRemoveAction(this@doAddOrRemoveMidiCCEventAction, id, points)
        )
    }
}

class MidiCCEventAddOrRemoveAction(
    private val events: MutableMidiCCEvents, private val id: Int, private var points: EnvelopePointList?
): ReversibleAction(points == null) {
    private val isDelete = points == null
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) events[id] = points!! else points = events.remove(id)
        return true
    }

    override val name = if (isDelete) "删除 CC 事件" else "添加 CC 事件"
    override val icon = if (isDelete) Icons.Filled.Add else Icons.Filled.Close
}