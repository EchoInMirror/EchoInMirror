package com.eimsound.daw.components.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import org.apache.commons.lang3.SystemUtils

private var isError = false
fun Modifier.onZoom(key: Any, onZoom: (magnification: Float, focal: Offset) -> Unit): Modifier {
    if (!isError && SystemUtils.IS_OS_MAC) try {
        return osxOnZoom(key, onZoom)
    } catch (e: Throwable) { isError = true }
    return this
}
