package cn.apisium.eim.api

interface Bus: Track {
    val tracks: List<Track>
    fun addTrack(track: Track, index: Int = -1)
}
