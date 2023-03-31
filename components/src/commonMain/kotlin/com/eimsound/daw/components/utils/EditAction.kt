package com.eimsound.daw.components.utils

import androidx.compose.ui.input.pointer.PointerIcon

enum class EditAction {
    NONE, MOVE, RESIZE, SELECT, DELETE;
    fun toPointerIcon(default: PointerIcon = PointerIcon.Default) = when(this) {
        MOVE -> PointerIcon.Move
        RESIZE -> PointerIcon.HorizontalResize
        else -> default
    }
}
