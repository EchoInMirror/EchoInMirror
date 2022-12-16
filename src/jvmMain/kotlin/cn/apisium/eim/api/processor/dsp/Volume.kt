package cn.apisium.eim.api.processor.dsp

interface Volume {
    var volume: Float
}

interface Mute {
    var isMute: Boolean
    fun stateChange()
}

interface Disabled {
    var isDisabled: Boolean
    fun stateChange()
}

interface Solo {
    var isSolo: Boolean
    fun stateChange()
}
