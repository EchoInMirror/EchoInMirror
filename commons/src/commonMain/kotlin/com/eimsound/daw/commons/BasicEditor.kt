package com.eimsound.daw.commons

interface BasicEditor {
    fun delete() { }
    fun copy() { }
    fun cut() {
        copy()
        delete()
    }
    fun paste() { }
    fun duplicate() { }
    val hasSelected get() = true
    val canPaste get() = true
    val canDelete get() = true
    val pasteValue: String? get() = null
}

interface MultiSelectableEditor : BasicEditor {
    fun selectAll() { }
}

interface SerializableEditor : BasicEditor {
    fun canPasteFromString(value: String) = true
    fun copyAsString(): String
    fun pasteFromString(value: String)
}
