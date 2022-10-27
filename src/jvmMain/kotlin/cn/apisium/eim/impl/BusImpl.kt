package cn.apisium.eim.impl

import cn.apisium.eim.api.Bus
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Track
import kotlin.collections.ArrayList

class BusImpl: TrackImpl("Bus"), Bus {
    private val _tracks = arrayListOf<Track>()
    override val tracks: List<Track> = _tracks

    override fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        _tracks.forEach { it.processBlock(buffers, position, midiBuffer) }
        super<TrackImpl>.processBlock(buffers, position, midiBuffer)
    }

    override fun close() {
        super<TrackImpl>.close()
        _tracks.forEach { it.close() }
        _tracks.clear()
    }

    override fun prepareToPlay() {
        super<TrackImpl>.prepareToPlay()
        _tracks.forEach { it.prepareToPlay() }
    }

    override fun addTrack(track: Track, index: Int) {
        if (index < 0) _tracks.add(track)
        else _tracks.add(index, track)
    }
}
