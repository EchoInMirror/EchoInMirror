package cn.apisium.eim.processor

import kotlin.math.pow

class Adsr {
    var attack = 0.0
        set(value) { field = value.coerceAtLeast(0.0) }
    var decay = 0.0
        set(value) { field = value.coerceAtLeast(0.0) }
    var sustain = 1.0
        set(value) { field = value.coerceIn(0.0, 1.0) }
    var release = 0.0
        set(value) { field = value.coerceAtLeast(0.0) }
    var attackTension = 0.0
        set(value) { field = value.coerceIn(0.0, 1.0) }
    var decayTension = 0.0
        set(value) { field = value.coerceIn(0.0, 1.0) }
    var releaseTension = 0.0
        set(value) { field = value.coerceIn(0.0, 1.0) }

    fun process(time: Float): Double {
        if (time < attack) {
            val t3 = time / attack
            return t3.pow(attackTension)
        }
        if (time < attack + decay) {
            val t3 = (time - attack) / decay
            val t4 = t3.pow(decayTension)
            return (1 - t4) * (1 - sustain) + sustain
        }
        if (time < attack + decay + sustain) {
            return sustain
        }
        if (time < attack + decay + sustain + release) {
            val t3 = (time - attack - decay - sustain) / release
            val t4 = t3.pow(releaseTension)
            return (1 - t4) * sustain
        }
        return 0.0
    }
}
