package com.eimsound.daw.components.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
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

@Composable
fun NativePainter(modifier: Modifier, vararg keys: Any?, block: Canvas.(size: Size, scope: CoroutineScope) -> Unit) {
    var image: Image? by remember { mutableStateOf(null) }
    var size by remember { mutableStateOf(Size.Zero) }
    val job: Array<Job?> = remember { arrayOf(null) }
    LaunchedEffect(*keys) {
        val curSize = size
        val (width, height) = curSize
        if (width <= 0 || height <= 0) {
            image = null
            return@LaunchedEffect
        }
        job[0]?.cancel()
        job[0] = launch(Dispatchers.Default) {
            val surface = Surface.makeRasterN32Premul(width.toInt(), height.toInt())
            surface.canvas.block(curSize, this)
            if (isActive) image = surface.makeImageSnapshot()
        }
    }
    Spacer(modifier.drawBehind {
        size = this.size
        image?.let { img -> drawIntoCanvas { it.nativeCanvas.drawImage(img, 0F, 0F) } }
    })
}
