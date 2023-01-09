package cn.apisium.eim.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ReversibleAction
import kotlinx.coroutines.runBlocking
import cn.apisium.eim.components.icons.PencilPlus
import cn.apisium.eim.components.icons.PencilMinus
import cn.apisium.eim.api.TrackClip
import cn.apisium.eim.api.processor.Track

fun doClipsAmountAction(clips: Collection<TrackClip<*>>, isDelete: Boolean) {
    runBlocking { EchoInMirror.undoManager.execute(ClipsAmountAction(clips.toList(), isDelete)) }
}

fun doClipsEditActionAction(clips: Collection<TrackClip<*>>, deltaX: Int = 0, deltaDuration: Int = 0,
                            newTracks: List<Track>? = null) {
    if (deltaX == 0 && deltaDuration == 0 && newTracks == null) return
    runBlocking { EchoInMirror.undoManager.execute(ClipsEditAction(clips.toList(), deltaX, deltaDuration, newTracks)) }
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
    private val clips: Collection<TrackClip<*>>,
    private val deltaX: Int, private val deltaDuration: Int,
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
            clips.forEach {
                it.time += x
                it.duration += duration
            }
        }
        if (newTracks != null) {
            if (isForward) clips.forEachIndexed { i, it ->
                it.track?.clips?.remove(it)
                newTracks[i].clips.add(it)
            } else clips.forEachIndexed { i, it ->
                it.track?.clips?.remove(it)
                originTracks[i]?.clips?.add(it)
            }
        }
        tracks.forEach {
            it.clips.update()
            it.onSuddenChange()
        }
        return true
    }
}
