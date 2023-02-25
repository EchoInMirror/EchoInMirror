package com.eimsound.daw.api.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

interface EditorExtension {
    val key: Any
    val isBackground: Boolean
    @Composable
    fun Content()
}

@Composable
fun List<EditorExtension>.EditorExtensions(isBackground: Boolean) {
    for (i in indices) {
        val it = this[i]
        if (it.isBackground == isBackground) key(it.key) { it.Content() }
    }
}
