package com.eimsound.daw.commons

interface IDisplayName {
    val displayName: String
}

val Any.displayName: String
    get() = when (this) {
        is IDisplayName -> displayName
        else -> toString()
    }
