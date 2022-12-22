package cn.apisium.eim.api


interface Renderable {
    val isRendering: Boolean
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Int>)
    fun onRenderStart()
    fun onRenderEnd()
}