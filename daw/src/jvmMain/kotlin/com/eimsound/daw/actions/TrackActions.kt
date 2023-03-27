package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.ListAddOrRemoveAction
import com.eimsound.daw.utils.ListReplaceAction
import com.eimsound.daw.utils.UndoableAction
import kotlinx.coroutines.runBlocking

class AddOrRemoveTrackAction(track: Track, target: MutableList<Track>,
                             isDelete: Boolean): ListAddOrRemoveAction<Track>(track, target, isDelete) {
    override val name = if (isDelete) "删除轨道" else "添加轨道"
    override val icon = if (isDelete) Icons.Filled.PlaylistAdd else Icons.Filled.Reorder
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

fun MutableList<AudioProcessor>.doAddOrRemoveAudioProcessorAction(audioProcessor: AudioProcessor,
                                                                  isDelete: Boolean = false, index: Int = -1) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            AudioProcessorAddOrRemoveAction(audioProcessor, this@doAddOrRemoveAudioProcessorAction, isDelete, index)
        )
    }
}

class AudioProcessorAddOrRemoveAction(track: AudioProcessor, target: MutableList<AudioProcessor>, isDelete: Boolean,
                                      index: Int = -1): ListAddOrRemoveAction<AudioProcessor>(track, target, isDelete, index) {
    override val name = if (isDelete) "删除音频处理器" else "添加音频处理器"
    override val icon = if (isDelete) Icons.Filled.Add else Icons.Filled.Close
}

fun MutableList<AudioProcessor>.doReplaceAudioProcessorAction(target: AudioProcessor, index: Int) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            AudioProcessorReplaceAction(target, this@doReplaceAudioProcessorAction, index)
        )
    }
}

class AudioProcessorReplaceAction(target: AudioProcessor, list: MutableList<AudioProcessor>,
                                  index: Int): ListReplaceAction<AudioProcessor>(target, list, index) {
    override val name = "音频处理器替换"
    override val icon = Icons.Filled.Autorenew
}
