package com.eimsound.daw.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

class ObservableMutableState<T>(initialValue: T, private val onChange: (T) -> Unit) {
    private var _value by mutableStateOf(initialValue)
    var value: T
        get() = _value
        set(value) {
            if (_value == value) return
            _value = value
            onChange(value)
        }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { this.value = value }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> observableMutableStateOf(initialValue: T, noinline onChange: (T) -> Unit) =
    ObservableMutableState(initialValue, onChange)
