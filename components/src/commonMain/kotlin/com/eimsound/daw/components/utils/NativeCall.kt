package com.eimsound.daw.components.utils

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.NativePointer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

private val nDrawRect: MethodHandle? = try {
    MethodHandles.lookup().unreflect(Class.forName("org.jetbrains.skia.CanvasKt")
        .getDeclaredMethod("_nDrawRect", NativePointer::class.java, Float::class.java,
            Float::class.java, Float::class.java, Float::class.java, NativePointer::class.java)
        .apply { isAccessible = true })
} catch (e: Exception) {
    e.printStackTrace()
    null
}

fun Canvas.drawRectNative(left: Float, top: Float, right: Float, bottom: Float, paint: org.jetbrains.skia.Paint) {
    val f = nDrawRect
    if (f == null) drawRect(Rect(left, top, right, bottom), paint)
    else {
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        f.invokeExact(_ptr, left, top, right, bottom, paint._ptr)
    }
}
