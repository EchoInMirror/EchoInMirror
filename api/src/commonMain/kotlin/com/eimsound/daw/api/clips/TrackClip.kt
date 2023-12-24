package com.eimsound.daw.api.clips

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.Disabled
import com.eimsound.daw.commons.IManualState
import com.eimsound.daw.commons.json.JsonSerializable
import kotlinx.serialization.Transient
import java.util.ArrayList

/**
 * @see com.eimsound.daw.impl.clips.TrackClipImpl
 */
interface TrackClip<T: Clip> : JsonSerializable, Disabled, Comparable<TrackClip<*>> {
    var time: Int
    var duration: Int
    var start: Int
    val clip: T
    @Transient
    var currentIndex: Int
    @Transient
    var track: Track?
    var color: Color?

    fun reset()
    fun copy(time: Int = this.time, duration: Int = this.duration, start: Int = this.start,
             clip: T = this.clip, currentIndex: Int = this.currentIndex, track: Track? = this.track
    ): TrackClip<T>
}

@Suppress("UNCHECKED_CAST", "unused")
fun TrackClip<*>.asMidiTrackClip() = this as TrackClip<MidiClip>
@Suppress("UNCHECKED_CAST")
fun TrackClip<*>.asMidiTrackClipOrNull() = if (clip is MidiClip) this as TrackClip<MidiClip> else null

interface TrackClipList : MutableList<TrackClip<*>>, IManualState

class DefaultTrackClipList(private val track: Track) : TrackClipList, ArrayList<TrackClip<*>>() {
    @Transient
    private var modification = mutableStateOf<Byte>(0)
    override fun update() { modification.value++ }
    override fun read() = modification.value
    override fun add(element: TrackClip<*>): Boolean {
        val r = super.add(element)
        element.track = track
        return r
    }
    override fun add(index: Int, element: TrackClip<*>) {
        super.add(index, element)
        element.track = track
    }
    override fun addAll(elements: Collection<TrackClip<*>>): Boolean {
        val r = super.addAll(elements)
        if (r) elements.forEach { it.track = track }
        return r
    }
    override fun addAll(index: Int, elements: Collection<TrackClip<*>>): Boolean {
        val r = super.addAll(index, elements)
        if (r) elements.forEach { it.track = track }
        return r
    }
    override fun remove(element: TrackClip<*>): Boolean {
        val r = super.remove(element)
        if (r) element.track = null
        return r
    }
    override fun removeAt(index: Int): TrackClip<*> {
        val r = super.removeAt(index)
        r.track = null
        return r
    }
    override fun removeAll(elements: Collection<TrackClip<*>>): Boolean {
        val r = super.removeAll(elements.toSet())
        if (r) elements.forEach { it.track = null }
        return r
    }
}
