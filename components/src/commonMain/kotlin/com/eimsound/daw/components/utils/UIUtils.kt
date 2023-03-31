package com.eimsound.daw.components.utils

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role

val PointerEvent.x get() = if (changes.isEmpty()) 0f else changes[0].position.x
val PointerEvent.y get() = if (changes.isEmpty()) 0f else changes[0].position.y

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickableWithIcon(enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null,
                               interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
                               indication: Indication = LocalIndication.current, onLongClick: (() -> Unit)? = null,
                               onDoubleClick: (() -> Unit)? = null, onClick: () -> Unit = { }): Modifier {
    val modifier = if (enabled) pointerHoverIcon(PointerIcon.Hand) else this
    return if (onLongClick != null || onDoubleClick != null) modifier.clickable(interactionSource, indication, enabled, onClickLabel, role) { }
        .combinedClickable(interactionSource, indication, onLongClick = onLongClick, onDoubleClick = onDoubleClick, onClick = onClick)
    else modifier.clickable(interactionSource, indication, enabled, onClickLabel, role, onClick)
}
