package com.eimsound.daw.components.gestures

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.eimsound.daw.utils.CurrentWindow
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.swing.JComponent
import javax.swing.SwingUtilities

private val GestureUtilities by lazy { Class.forName("com.apple.eawt.event.GestureUtilities") }
private val GestureListener by lazy { Class.forName("com.apple.eawt.event.GestureListener") }
private val addGestureListenerTo by lazy {
    GestureUtilities.getMethod("addGestureListenerTo", JComponent::class.java, GestureListener).apply {
        isAccessible = true
    }
}
private val removeGestureListenerFrom by lazy {
    GestureUtilities.getMethod("removeGestureListenerFrom", JComponent::class.java, GestureListener)
}
private val MagnificationEvent by lazy { Class.forName("com.apple.eawt.event.MagnificationEvent") }
private val getMagnification by lazy { MagnificationEvent.getMethod("getMagnification") }

internal fun Modifier.osxOnZoom(key: Any, onZoom: (magnification: Float, focal: Offset) -> Unit): Modifier {
    addGestureListenerTo
    return composed {
        val window = CurrentWindow.current
        val rootPane = remember(key, window) { SwingUtilities.getRootPane(window) }
        var (lastPointer) = remember(key) { arrayOf<Offset?>(null) }

        DisposableEffect(key, rootPane) {
            val listener = Proxy.newProxyInstance(
                GestureUtilities.classLoader,
                arrayOf(Class.forName("com.apple.eawt.event.MagnificationListener"))
            ) { obj, method, params ->
                if (method.name != "magnify") return@newProxyInstance InvocationHandler.invokeDefault(obj, method, params)
                if (params[0] == null) return@newProxyInstance null
                if (lastPointer != null) onZoom((getMagnification.invoke(params[0]) as Double).toFloat(), lastPointer!!)
                return@newProxyInstance null
            }
            addGestureListenerTo.invoke(null, rootPane, listener)
            onDispose {
                removeGestureListenerFrom.invoke(null, rootPane, listener)
            }
        }

        pointerInput(key) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    lastPointer = if (event.type == PointerEventType.Exit) null
                    else event.changes.first().position
                }
            }
        }
    }
}