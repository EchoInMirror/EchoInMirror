@file:Suppress("unused")

package com.eimsound.daw.utils

import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

interface IManualState {
    fun update()
    fun read()
}

open class ManualState : IManualState {
    @Transient private var modification = mutableStateOf(0)
    override fun update() { modification.value++ }
    override fun read() { modification.value }
}

interface IManualStateValue <T> : IManualState {
    var value: T
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    fun readValue(): T {
        read()
        return value
    }
}

inline fun <T> IManualStateValue<T>.updateOn(block: (T) -> Unit) {
    block(value)
    update()
}

inline fun <T> IManualStateValue<T>.updateWith(block: (T) -> T) {
    value = block(value)
    update()
}

inline fun <T, R> IManualStateValue<T>.readWith(block: (T) -> R): R {
    read()
    return block(value)
}

class ManualStateValue<T>(override var value: T): IManualStateValue<T>, ManualState() {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        read()
        return value
    }
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (value == this.value) return
        this.value = value
        update()
    }
}
