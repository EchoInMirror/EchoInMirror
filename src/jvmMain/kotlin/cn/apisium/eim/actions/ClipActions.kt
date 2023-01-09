package cn.apisium.eim.actions

import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.ReversibleAction
import kotlinx.coroutines.runBlocking
import cn.apisium.eim.components.icons.PencilPlus
import cn.apisium.eim.components.icons.PencilMinus
import cn.apisium.eim.api.TrackClip

fun doNoteAmountAction(clips: Collection<TrackClip<*>>, isDelete: Boolean) {
    runBlocking { EchoInMirror.undoManager.execute(ClipAmountAction(clips, isDelete)) }
}

class ClipAmountAction(private val clips: Collection<TrackClip<*>>, isDelete: Boolean) :
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
