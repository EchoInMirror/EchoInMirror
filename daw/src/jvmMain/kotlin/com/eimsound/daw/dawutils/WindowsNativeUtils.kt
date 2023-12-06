package com.eimsound.daw.dawutils

import androidx.compose.ui.graphics.Color
import java.lang.foreign.Addressable
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySession
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import kotlin.math.roundToLong

private val session by lazy { MemorySession.openImplicit() }
private val DwmSetWindowAttribute by lazy {
    val linker = Linker.nativeLinker()
    val kernel = SymbolLookup.libraryLookup("dwmapi.dll", session)
    linker.downcallHandle(
        kernel.lookup("DwmSetWindowAttribute").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT
        )
    )
}

internal enum class WindowColorType(val value: Int) {
    CAPTION(35), BORDER(34), TEXT(36);
}
internal fun windowsSetWindowColor(handle: Long, color: Color, type: WindowColorType = WindowColorType.CAPTION) = try {
    (DwmSetWindowAttribute.invokeExact(handle, type.value,
        session.allocate(
            ValueLayout.JAVA_LONG,
            (color.blue * 255).roundToLong() shl 16 or ((color.green * 255).roundToLong() shl 8) or ((color.red * 255).roundToLong() shl 0)
        ) as Addressable, 4) as Long) >= 0
} catch (ignored: Throwable) {
    false
}