package com.eimsound.daw.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.util.fastForEach
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.TrackClip
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.actions.ListDisabledAction
import com.eimsound.daw.components.icons.PencilMinus
import com.eimsound.daw.components.icons.PencilPlus
import com.eimsound.daw.commons.actions.ReversibleAction
import kotlinx.coroutines.runBlocking

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

fun List<TrackClip<*>>.doClipsDisabledAction(isDisabled: Boolean = false) {
    runBlocking { EchoInMirror.undoManager.execute(ClipsDisabledAction(this@doClipsDisabledAction, isDisabled)) }
}

class ClipsDisabledAction(clips: List<TrackClip<*>>, isDisabled: Boolean = false) : ListDisabledAction(clips, isDisabled) {
    override val name = if (isDisabled) "片段禁用 (${clips.size}个)" else "片段启用 (${clips.size}个)"
    override val icon = Icons.Default.Edit
}
