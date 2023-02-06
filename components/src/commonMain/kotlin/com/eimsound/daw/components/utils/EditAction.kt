package com.eimsound.daw.components.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults

enum class EditAction {
    NONE, MOVE, RESIZE, SELECT, DELETE;
    @OptIn(ExperimentalComposeUiApi::class)
    fun toPointerIcon(default: PointerIcon = PointerIconDefaults.Default) = when(this) {
        MOVE -> PointerIconDefaults.Move
        RESIZE -> PointerIconDefaults.HorizontalResize
        else -> default
    }
}
