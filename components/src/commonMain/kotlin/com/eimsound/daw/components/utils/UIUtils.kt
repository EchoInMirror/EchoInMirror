package com.eimsound.daw.components.utils

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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

@Suppress("DEPRECATION")
fun Modifier.onRightClickOrLongPress(onClick: (Offset, PointerKeyboardModifiers) -> Unit) = composed {
    var (position) = remember { arrayOf(Offset.Zero) }
    onGloballyPositioned { position = it.positionInWindow() }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Release) {
                    event.changes.first().consume()
                    continue
                }
                if (event.type != PointerEventType.Press) continue
                val change = event.changes.first()
                change.consumed.positionChange = false
                if (event.buttons.isPrimaryPressed) {
                    try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation(PointerEventPass.Initial)
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (!change.consumed.positionChange) {
                            onClick(change.position + position, event.keyboardModifiers)
                            change.consume()
                            change.consumed.positionChange = true
                            change.changedToDown()
                        }
                    }
                } else if (event.buttons.isSecondaryPressed && !change.isConsumed) {
                    onClick(change.position + position, event.keyboardModifiers)
                    change.consume()
                }
            }
        }
    }
}
