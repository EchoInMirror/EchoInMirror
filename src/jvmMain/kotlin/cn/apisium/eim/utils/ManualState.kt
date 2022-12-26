package cn.apisium.eim.utils

import androidx.compose.runtime.mutableStateOf

open class ManualState {
    private var modification = mutableStateOf(0)
    fun update() { modification.value++ }
    fun read() { modification.value }
}
