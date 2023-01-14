package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Reorder
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.ReversibleAction
import com.eimsound.daw.utils.UndoableAction
import kotlinx.coroutines.runBlocking

class AddOrRemoveTrackAction(private val track: Track, private val target: MutableList<Track>,
                             private val isDelete: Boolean): ReversibleAction(isDelete) {
    private var index = -1

    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            if (isDelete) {
                index = target.indexOf(track)
                target.remove(track)
            } else target.add(track)
        } else {
            if (isDelete) target.add(index, track)
            else target.remove(track)
        }
        return true
    }

    override val name = "添加或删除轨道"
    override val icon = Icons.Filled.PlaylistAdd
}

fun Track.doAddOrRemoveTrackAction(target: MutableList<Track>, isDelete: Boolean = false) {
    runBlocking { EchoInMirror.undoManager.execute(AddOrRemoveTrackAction(this@doAddOrRemoveTrackAction,
        target, isDelete)) }
}

class ReorderTrackAction(private val target: Track, private val sourceTracks: MutableList<Track>,
                         private val destTracks: MutableList<Track>, private val destIndex: Int = 0): UndoableAction {
    private var sourceIndex = -1
    override suspend fun undo(): Boolean {
        if (sourceIndex == -1) return false
        destTracks.remove(target)
        sourceTracks.add(sourceIndex, target)
        return true
    }

    override suspend fun execute(): Boolean {
        sourceIndex = sourceTracks.indexOf(target)
        if (sourceIndex == -1 || !sourceTracks.remove(target)) return false
        destTracks.add(destIndex, target)
        return true
    }

    override val name = "轨道重新排序"
    override val icon = Icons.Filled.Reorder
}

fun Track.doReorderAction(sourceTracks: MutableList<Track>, destTracks: MutableList<Track>, destIndex: Int = 0) {
    runBlocking { EchoInMirror.undoManager.execute(ReorderTrackAction(this@doReorderAction,
        sourceTracks, destTracks, destIndex)) }
}
