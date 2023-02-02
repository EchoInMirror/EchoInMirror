@file:Suppress("PrivatePropertyName")

package com.eimsound.daw.components.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skiko.Cursor

val BorderCornerRadius2PX = CornerRadius(2f)
val Stroke1PX = Stroke(1f)
val Stroke2PX = Stroke(2f)
val Stroke1_5PX = Stroke(1.5f)
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