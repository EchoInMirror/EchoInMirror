package cn.apisium.eim.impl

import cn.apisium.eim.api.Bus
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.Track
import kotlin.collections.ArrayList

class BusImpl: TrackImpl("Bus"), Bus {
    override val tracks = arrayListOf<Track>()

    override suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>?) {
        tracks.forEach { it.processBlock(buffers, position, midiBuffer) }
        super<TrackImpl>.processBlock(buffers, position, midiBuffer)
    }

    override fun close() {
        super<TrackImpl>.close()
        tracks.forEach { it.close() }
        tracks.clear()
    }

    override fun prepareToPlay() {
        super<TrackImpl>.prepareToPlay()
        tracks.forEach { it.prepareToPlay() }
    }

    override fun addTrack(track: Track, index: Int) {
        if (index < 0) tracks.add(track)
        else tracks.add(index, track)
    }
}
