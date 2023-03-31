package com.eimsound.daw.components.utils

import androidx.compose.ui.input.pointer.PointerIcon
import org.jetbrains.skiko.Cursor

private val HORIZONTAL_RESIZE_CURSOR = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
private val VERTICAL_RESIZE_CURSOR = PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))
private val MOVE_CURSOR = PointerIcon(Cursor(Cursor.MOVE_CURSOR))

val PointerIcon.Companion.HorizontalResize get() = HORIZONTAL_RESIZE_CURSOR
val PointerIcon.Companion.VerticalResize get() = VERTICAL_RESIZE_CURSOR
val PointerIcon.Companion.Move get() = MOVE_CURSOR
