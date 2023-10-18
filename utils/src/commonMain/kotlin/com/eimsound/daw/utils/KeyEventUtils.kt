package com.eimsound.daw.utils

import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import org.apache.commons.lang3.SystemUtils

val PointerKeyboardModifiers.isCrossPlatformCtrlPressed get() = if (SystemUtils.IS_OS_MAC) isMetaPressed else isCtrlPressed
val PointerKeyboardModifiers.isCrossPlatformAltPressed get() = if (SystemUtils.IS_OS_MAC) isCtrlPressed else isMetaPressed
val KeyEvent.isCrossPlatformCtrlPressed get() = if (SystemUtils.IS_OS_MAC) isMetaPressed else isCtrlPressed
val KeyEvent.isCrossPlatformAltPressed get() = if (SystemUtils.IS_OS_MAC) isCtrlPressed else isMetaPressed
