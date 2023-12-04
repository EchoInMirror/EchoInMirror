package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.api.clips.ClipManager
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.actions.ListDisabledAction
import com.eimsound.daw.components.icons.PencilMinus
import com.eimsound.daw.components.icons.PencilPlus
import com.eimsound.daw.commons.actions.ReversibleAction
import com.eimsound.daw.commons.actions.UndoableAction
import kotlinx.coroutines.runBlocking
import java.util.LinkedList

fun Collection<TrackClip<*>>.doClipsAmountAction(isDelete: Boolean) {
    runBlocking { EchoInMirror.undoManager.execute(ClipsAmountAction(toList(), isDelete)) }
}

fun Collection<TrackClip<*>>.doClipsEditActionAction(deltaX: Int = 0, deltaDuration: Int = 0,
                                                     deltaStart: Int = 0, newTracks: List<Track>? = null) {
    if (deltaX == 0 && deltaDuration == 0 && deltaStart == 0 && newTracks == null) return
    runBlocking { EchoInMirror.undoManager.execute(ClipsEditAction(toList(), deltaX, deltaDuration, deltaStart, newTracks)) }
}

class ClipsAmountAction(private val clips: Collection<TrackClip<*>>, isDelete: Boolean) :
    ReversibleAction(isDelete) {
    override val name = (if (isDelete) "片段删除 (" else "片段添加 (") + clips.size + "个)"
    override val icon = if (isDelete) PencilMinus else PencilPlus
    private val originTracks = clips.map { it.track }
    private val tracks = originTracks.toSet()
    override suspend fun perform(isForward: Boolean): Boolean {
        if (isForward) {
            clips.forEachIndexed { i, it ->
                originTracks[i]?.clips?.add(it)
                it.reset()
            }
            tracks.forEach { it?.clips?.let { trackClips ->
                trackClips.sort()
                trackClips.update()
                it.onSuddenChange()
            } }
        } else {
            clips.forEachIndexed { i, it ->
                originTracks[i]?.clips?.remove(it)
                it.reset()
            }
            tracks.forEach { it?.clips?.let { trackClips ->
                trackClips.update()
                it.onSuddenChange()
            } }
        }
        return true
    }
}

class ClipsEditAction(
    private val clips: List<TrackClip<*>>,
    private val deltaX: Int, private val deltaDuration: Int, private val deltaStart: Int = 0,
    private val newTracks: List<Track>? = null
) : ReversibleAction() {
    override val name = "片段编辑 (${clips.size}个)"
    override val icon = Icons.Default.Edit
    private val originTracks = clips.map { it.track }
    private val tracks = hashSetOf<Track>()

    init {
        clips.forEach {
            val track = it.track ?: return@forEach
            tracks.add(track)
        }
        if (newTracks != null) tracks.addAll(newTracks)
    }

    override suspend fun perform(isForward: Boolean): Boolean {
        if (deltaX != 0 || deltaDuration != 0) {
            val x = if (isForward) deltaX else -deltaX
            val duration = if (isForward) deltaDuration else -deltaDuration
            val start = if (isForward) deltaStart else -deltaStart
            clips.fastForEach {
                if (x != 0) it.time += x
                if (duration != 0) it.duration += duration
                if (start != 0) it.start += start
            }
        }
        if (newTracks != null) {
            if (isForward) clips.forEachIndexed { i, it ->
                it.track?.clips?.remove(it)
                newTracks[i].clips.add(it)
                it.track = newTracks[i]
            } else clips.forEachIndexed { i, it ->
                it.track?.clips?.remove(it)
                originTracks[i]?.clips?.add(it)
                it.track = originTracks[i]
            }
        }
        tracks.forEach {
            it.clips.update()
            it.onSuddenChange()
        }
        return true
    }
}

fun List<TrackClip<*>>.doClipsDisabledAction(isDisabled: Boolean? = null) {
    runBlocking { EchoInMirror.undoManager.execute(ClipsDisabledAction(this@doClipsDisabledAction, isDisabled)) }
}

class ClipsDisabledAction(clips: List<TrackClip<*>>, isDisabled: Boolean? = null) : ListDisabledAction(clips, isDisabled) {
    override val name = if ((isDisabled ?: clips.firstOrNull()?.isDisabled?.let { !it }) != false)
        "片段禁用 (${clips.size}个)" else "片段启用 (${clips.size}个)"
    override val icon = Icons.Default.Edit
}

fun List<TrackClip<*>>.doClipsSplitAction(clickTime: Int) {
    runBlocking { EchoInMirror.undoManager.execute(ClipsSplitAction(this@doClipsSplitAction, clickTime)) }
}

class ClipsSplitAction(private val clips: List<TrackClip<*>>, private val clickTime: Int) : UndoableAction {
    override val name = "片段分割 (${clips.size}个)"
    override val icon = Icons.Default.ContentCut
    private var reverts: List<ClipSplitRevert>? = null

    private data class ClipSplitRevert(
        val clip: TrackClip<*>, val oldDuration: Int, val revert: () -> Unit, val newClip: TrackClip<*>
    )

    override suspend fun undo(): Boolean {
        val list = reverts ?: return false
        list.fastForEach {
            it.revert()
            val track = it.clip.track ?: return@fastForEach
            it.clip.duration = it.oldDuration
            track.clips.remove(it.newClip)
            track.clips.update()
        }
        reverts = null
        return true
    }

    override suspend fun execute(): Boolean {
        reverts = clips.mapNotNull { clip ->
            val track = clip.track ?: return@mapNotNull null
            val result = clip.clip.factory.split(@Suppress("TYPE_MISMATCH") clip, clickTime)
            val newClip = ClipManager.instance.createTrackClip(
                result.clip, clip.time + clickTime, clip.duration - clickTime, result.start, track
            )
            track.clips.add(newClip)
            val oldDuration = clip.duration
            clip.duration = clickTime
            track.clips.sort()
            track.clips.update()
            return@mapNotNull ClipSplitRevert(clip, oldDuration, result::revert, newClip)
        }
        return true
    }
}

fun List<TrackClip<*>>.doClipsMergeAction() {
    runBlocking { EchoInMirror.undoManager.execute(ClipsMergeAction(this@doClipsMergeAction)) }
}

class ClipsMergeAction(clips: List<TrackClip<*>>) : UndoableAction {
    override val name = "片段合并 (${clips.size}个)"
    override val icon = Icons.Default.ContentCut
    private val clips = hashMapOf<TrackClip<*>, Track>().apply {
        clips.fastForEach {
            if (it.clip.factory.canMerge(it) && it.track != null) put(it, it.track!!)
        }
    }
    private val newClips: MutableMap<Track, MutableList<TrackClip<*>>> = hashMapOf()

    override suspend fun undo(): Boolean {
        newClips.forEach { (track, clips) ->
            track.clips.removeAll(clips)
        }
        newClips.clear()

        val tracks = hashSetOf<Track>()
        clips.forEach { (clip, track) ->
            track.clips.add(clip)
            tracks.add(track)
        }
        tracks.forEach {
            it.clips.sort()
            it.clips.update()
        }
        return true
    }

    override suspend fun execute(): Boolean {
        val map = hashMapOf<Track, MutableList<TrackClip<*>>>()
        clips.forEach { (clip, track) ->
            map.getOrPut(track) { LinkedList() }.add(clip)
        }
        map.forEach { (track, clips) ->
            var size = 0
            while (size != clips.size) {
                size = clips.size
                if (size < 2) break
                val currentClipFactory = clips.first().clip.factory
                val canMerges = mutableSetOf<TrackClip<*>>()
                clips.removeIf {
                    if (currentClipFactory.canMerge(it)) {
                        canMerges.add(it)
                        true
                    } else false
                }
                if (canMerges.size < 2) {
                    this.clips.remove(canMerges.firstOrNull() ?: continue)
                }
                track.clips.removeAll(canMerges)
                currentClipFactory.merge(canMerges).fastForEach {
                    val trackClip = ClipManager.instance.createTrackClip(it.clip, it.time, it.duration, it.start, track)
                    track.clips.add(trackClip)
                    newClips.getOrPut(track) { mutableListOf() }.add(trackClip)
                }
                track.clips.sort()
                track.clips.update()
            }
        }
        return true
    }
}
