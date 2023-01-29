package com.eimsound.audioprocessor.data

import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.utils.IManualState
import com.fasterxml.jackson.annotation.JsonIgnore

data class EnvelopePoint(
    val time: Int,
    val value: Float,
    val tension: Float = 0F
)

interface EnvelopePointList : MutableList<EnvelopePoint>, IManualState {
    fun sort()
}

class DefaultEnvelopePointList : EnvelopePointList, ArrayList<EnvelopePoint>() {
    @JsonIgnore
    private var modification = mutableStateOf(0)
    override fun sort() = sortBy { it.time }
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}


