package com.eimsound.audioprocessor.dsp

interface Volume {
    var volume: Float
}

interface Disabled {
    var isDisabled: Boolean
}

interface Solo {
    var isSolo: Boolean
}
