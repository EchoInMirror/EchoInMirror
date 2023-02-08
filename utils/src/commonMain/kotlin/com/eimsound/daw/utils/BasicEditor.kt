package com.eimsound.daw.utils

interface BasicEditor {
    fun delete() { }
    fun copy() { }
    fun cut() {
        copy()
        delete()
    }
    fun paste() { }
    fun selectAll() { }
    fun hasSelected(): Boolean = true
    fun canPaste(): Boolean = true
}

interface SerializableEditor : BasicEditor {
    fun copyAsString(): String
    fun pasteFromString(value: String)
}
