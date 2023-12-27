package com.eimsound.daw.components.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import org.jetbrains.skia.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skiko.context.*
import org.jetbrains.skia.impl.NativePointer
import java.awt.Window
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.util.WeakHashMap

private val nDrawRect: MethodHandle? = try {
    MethodHandles.lookup().unreflect(Class.forName("org.jetbrains.skia.CanvasKt")
        .getDeclaredMethod("_nDrawRect", NativePointer::class.java, Float::class.java,
            Float::class.java, Float::class.java, Float::class.java, NativePointer::class.java)
        .apply { isAccessible = true })
} catch (e: Exception) {
    e.printStackTrace()
    null
}

fun Canvas.drawRectNative(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
    val f = nDrawRect
    if (f == null) drawRect(Rect(left, top, right, bottom), paint)
    else {
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        f.invokeExact(_ptr, left, top, right, bottom, paint._ptr)
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private object SurfaceUtil {
    private lateinit var ComposeWindow_delegate: Field
    private lateinit var ComposeWindowDelegate_bridge: Field
    private val ContextHandlerSurface: MethodHandle
    private var redrawerCache: MutableMap<Window, ContextHandler>? = null

    init {
        var contextHandlerSurface: MethodHandle? = null
        try {
            ComposeWindow_delegate = ComposeWindow::class.java.getDeclaredField("delegate").apply { isAccessible = true }
            ComposeWindowDelegate_bridge = ComposeWindowDelegate::class.java.getDeclaredField("_bridge").apply { isAccessible = true }
            contextHandlerSurface = MethodHandles.lookup().unreflectGetter(ContextHandler::class.java.getDeclaredField("surface").apply { isAccessible = true })
            redrawerCache = WeakHashMap()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ContextHandlerSurface = contextHandlerSurface ?: MethodHandles.empty(MethodType.methodType(Surface::class.java))
    }

    fun getSurface(window: Window): Surface? {
        val cache = redrawerCache ?: return null
        var ctx = cache[window]
        if (null == ctx) try {
            val redrawer = (ComposeWindowDelegate_bridge.get(ComposeWindow_delegate.get(window)) as WindowComposeBridge).component.redrawer!!
            ctx = redrawer::class.java.getDeclaredField("contextHandler").apply { isAccessible = true }.get(redrawer) as ContextHandler
            cache[window] = ctx
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (null == ctx) return null
        val surface = ContextHandlerSurface.invokeExact(ctx) as Surface
        return if (surface.isClosed) null else surface
    }
}

@Composable
fun NativePainter(modifier: Modifier, block: Canvas.(size: Size) -> Unit) {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    val window = androidx.compose.ui.window.LocalWindow.current
    Spacer(modifier.drawWithCache {
        var image: Image? = null
        if (window != null && size.width != 0F && size.height != 0F) {
            SurfaceUtil.getSurface(window)?.makeSurface(size.width.toInt(), size.height.toInt())?.apply {
                canvas.block(size)
                image = makeImageSnapshot()
                close()
            }
        }
        onDrawBehind {
            image?.apply {
                drawContext.canvas.nativeCanvas.drawImage(this, 0F, 0F)
                if (!isClosed) close()
            }
        }
    })
}
