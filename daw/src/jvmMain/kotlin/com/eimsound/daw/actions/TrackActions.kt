package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.eimsound.audioprocessor.AudioProcessor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.DefaultTrackAudioProcessorWrapper
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.commons.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AddOrRemoveTrackAction(track: Track, target: MutableList<Track>,
                             isDelete: Boolean, index: Int): ListAddOrRemoveAction<Track>(track, target, isDelete, index) {
    override val name = if (isDelete) "删除轨道" else "添加轨道"
    override val icon = if (isDelete) Icons.Filled.PlaylistAdd else Icons.Filled.Reorder
}

fun MutableList<Track>.doAddOrRemoveTrackAction(target: Track, isDelete: Boolean = false, index: Int = -1) {
    runBlocking {
        EchoInMirror.undoManager.execute(AddOrRemoveTrackAction(target, this@doAddOrRemoveTrackAction, isDelete, index))
    }
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

fun MutableList<TrackAudioProcessorWrapper>.doAddOrRemoveAudioProcessorAction(audioProcessor: AudioProcessor,
                                                                              isDelete: Boolean = false, index: Int = -1) {
    doAddOrRemoveAudioProcessorAction(DefaultTrackAudioProcessorWrapper(audioProcessor), isDelete, index)
}
@OptIn(DelicateCoroutinesApi::class)
fun MutableList<TrackAudioProcessorWrapper>.doAddOrRemoveAudioProcessorAction(audioProcessor: TrackAudioProcessorWrapper,
                                                                              isDelete: Boolean = false, index: Int = -1) {
    GlobalScope.launch {
        EchoInMirror.undoManager.execute(
            AudioProcessorAddOrRemoveAction(audioProcessor, this@doAddOrRemoveAudioProcessorAction, isDelete, index)
        )
    }
}

fun MutableList<TrackAudioProcessorWrapper>.doMoveAudioProcessorAction(index: Int, to: MutableList<TrackAudioProcessorWrapper>,
                                                                       toIndex: Int = -1) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            AudioProcessorMoveAction(index, this@doMoveAudioProcessorAction, to, toIndex)
        )
    }
}

class AudioProcessorAddOrRemoveAction(target: TrackAudioProcessorWrapper, source: MutableList<TrackAudioProcessorWrapper>, isDelete: Boolean,
                                      index: Int = -1): ListAddOrRemoveAction<TrackAudioProcessorWrapper>(target, source, isDelete, index) {
    override val name = if (isDelete) "删除音频处理器" else "添加音频处理器"
    override val icon = if (isDelete) Icons.Filled.Close else Icons.Filled.Add
}

class AudioProcessorMoveAction(
    index: Int, from: MutableList<TrackAudioProcessorWrapper>, to: MutableList<TrackAudioProcessorWrapper>, toIndex: Int = -1
) : ListElementMoveAction<TrackAudioProcessorWrapper>(index, from, to, toIndex) {
    override val name = "移动音频处理器"
    override val icon = Icons.Filled.Reorder
}

fun MutableList<TrackAudioProcessorWrapper>.doReplaceAudioProcessorAction(target: AudioProcessor, index: Int) {
    doReplaceAudioProcessorAction(DefaultTrackAudioProcessorWrapper(target), index)
}
fun MutableList<TrackAudioProcessorWrapper>.doReplaceAudioProcessorAction(target: TrackAudioProcessorWrapper, index: Int) {
    runBlocking {
        EchoInMirror.undoManager.execute(
            AudioProcessorReplaceAction(target, this@doReplaceAudioProcessorAction, index)
        )
    }
}

class AudioProcessorReplaceAction(target: TrackAudioProcessorWrapper, list: MutableList<TrackAudioProcessorWrapper>,
                                  index: Int): ListReplaceAction<TrackAudioProcessorWrapper>(target, list, index) {
    override val name = "音频处理器替换"
    override val icon = Icons.Filled.Autorenew
}
