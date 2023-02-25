@file:Suppress("PrivatePropertyName")

package com.eimsound.daw.components.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skiko.Cursor

private val HORIZONTAL_RESIZE_CURSOR = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
private val VERTICAL_RESIZE_CURSOR = PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))
private val MOVE_CURSOR = PointerIcon(Cursor(Cursor.MOVE_CURSOR))
private val ZeroDp = 0.dp

@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalComposeUiApi::class)
val PointerIconDefaults.HorizontalResize get() = HORIZONTAL_RESIZE_CURSOR
@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalComposeUiApi::class)
val PointerIconDefaults.VerticalResize get() = VERTICAL_RESIZE_CURSOR
@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalComposeUiApi::class)
val PointerIconDefaults.Move get() = MOVE_CURSOR
val Dp.Companion.Zero get() = ZeroDp
