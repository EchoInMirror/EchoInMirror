package com.eimsound.daw.components.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role

val PointerEvent.x get() = if (changes.isEmpty()) 0f else changes[0].position.x
val PointerEvent.y get() = if (changes.isEmpty()) 0f else changes[0].position.y

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
fun Modifier.clickableWithIcon(enabled: Boolean = true, onClickLabel: String? = null,
                               role: Role? = null, onLongClick: (() -> Unit)? = null,
                               onDoubleClick: (() -> Unit)? = null, onClick: () -> Unit = { }): Modifier {
    val modifier = if (enabled) pointerHoverIcon(PointerIconDefaults.Hand) else this
    return if (onLongClick != null || onDoubleClick != null) modifier.clickable(enabled, onClickLabel, role) { }
        .combinedClickable(onLongClick = onLongClick, onDoubleClick = onDoubleClick, onClick = onClick)
    else modifier.clickable(enabled, onClickLabel, role, onClick)
}
