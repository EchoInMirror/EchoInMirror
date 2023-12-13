@file:Suppress("FunctionName")

package com.eimsound.daw.dawutils

import androidx.compose.ui.graphics.Color
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.ptr.LongByReference
import kotlin.math.roundToLong

private interface DwmAPI : Library {
    companion object {
        val INSTANCE: DwmAPI = Native.load("dwmapi", DwmAPI::class.java)
    }

    @Suppress("unused")
    @Structure.FieldOrder("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
    class Margins(
        @JvmField
        var cxLeftWidth: Int,
        @JvmField
        var cxRightWidth: Int,
        @JvmField
        var cyTopHeight: Int,
        @JvmField
        var cyBottomHeight: Int
    ) : Structure(), Structure.ByReference

    fun DwmSetWindowAttribute(hWnd: Long, dwAttribute: Int, pvAttribute: LongByReference, cbAttribute: Int): Int

    fun DwmExtendFrameIntoClientArea(hWnd: Long, pMarInset: Margins): Int
}

internal enum class WindowColorType(val value: Int) {
    CAPTION(35), BORDER(34), TEXT(36);
}
internal fun windowsSetWindowColor(handle: Long, color: Color, type: WindowColorType = WindowColorType.CAPTION) = try {
    DwmAPI.INSTANCE.DwmSetWindowAttribute(
        handle, type.value,
        LongByReference((color.blue * 255).roundToLong() shl 16 or ((color.green * 255).roundToLong() shl 8) or ((color.red * 255).roundToLong() shl 0)),
        4
    ) >= 0
} catch (e: Throwable) {
    e.printStackTrace()
    false
}

internal fun windowsDecorateWindow(handle: Long) = try {
    DwmAPI.INSTANCE.DwmSetWindowAttribute(
        handle, 2,
        LongByReference(2),
        4
    ) >= 0 &&
    DwmAPI.INSTANCE.DwmExtendFrameIntoClientArea(handle, DwmAPI.Margins(-1, -1, -1, -1)) >= 0
} catch (e: Throwable) {
    e.printStackTrace()
    false
}
