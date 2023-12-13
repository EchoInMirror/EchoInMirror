package com.eimsound.daw.dawutils

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.Panel
import org.apache.commons.lang3.SystemUtils
import org.ocpsoft.prettytime.PrettyTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val ZERO_DATE = Date(0)
var CLIPBOARD_MANAGER: ClipboardManager? = null
val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
val TIME_PRETTIER = PrettyTime()
val DURATION_PRETTIER = PrettyTime(ZERO_DATE)

data class Border(val strokeWidth: Dp, val color: Color, val offset: Dp = 0.dp)

fun Modifier.border(
    start: Border? = null,
    top: Border? = null,
    end: Border? = null,
    bottom: Border? = null,
) =
    drawBehind {
        start?.let {
            drawStartBorder(it, shareTop = top != null, shareBottom = bottom != null)
        }
        top?.let {
            drawTopBorder(it, shareStart = start != null, shareEnd = end != null)
        }
        end?.let {
            drawEndBorder(it, shareTop = top != null, shareBottom = bottom != null)
        }
        bottom?.let {
            drawBottomBorder(border = it, shareStart = start != null, shareEnd = end != null)
        }
    }

private fun DrawScope.drawTopBorder(
    border: Border,
    shareStart: Boolean = true,
    shareEnd: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(if (shareStart) strokeWidthPx else 0f, strokeWidthPx)
            val width = size.width
            lineTo(if (shareEnd) width - strokeWidthPx else width, strokeWidthPx)
            lineTo(width, 0f)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawBottomBorder(
    border: Border,
    shareStart: Boolean,
    shareEnd: Boolean
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            val width = size.width
            val height = size.height
            moveTo(0f, height)
            lineTo(if (shareStart) strokeWidthPx else 0f, height - strokeWidthPx)
            lineTo(if (shareEnd) width - strokeWidthPx else width, height - strokeWidthPx)
            lineTo(width, height)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawStartBorder(
    border: Border,
    shareTop: Boolean = true,
    shareBottom: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    val offset = border.offset.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            moveTo(offset, 0f)
            lineTo(strokeWidthPx + offset, if (shareTop) strokeWidthPx else 0f)
            val height = size.height
            lineTo(strokeWidthPx + offset, if (shareBottom) height - strokeWidthPx else height)
            lineTo(offset, height)
            close()
        },
        color = border.color
    )
}

private fun DrawScope.drawEndBorder(
    border: Border,
    shareTop: Boolean = true,
    shareBottom: Boolean = true
) {
    val strokeWidthPx = border.strokeWidth.toPx()
    if (strokeWidthPx == 0f) return
    drawPath(
        Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width, 0f)
            lineTo(width - strokeWidthPx, if (shareTop) strokeWidthPx else 0f)
            lineTo(width - strokeWidthPx, if (shareBottom) height - strokeWidthPx else height)
            lineTo(width, height)
            close()
        },
        color = border.color
    )
}

var ScrollState.openMaxValue
    get() = maxValue
    @Suppress("INVISIBLE_SETTER")
    set(value) { maxValue = value }

fun formatDuration(time: Long): String = DURATION_PRETTIER.formatDuration(Date(time)).ifEmpty { DURATION_PRETTIER.format(
    ZERO_DATE
) }

fun randomColor() = com.eimsound.daw.components.utils.randomColor(!EchoInMirror.windowManager.isDarkTheme)

@Suppress("unused")
fun Panel.isActive() = EchoInMirror.windowManager.activePanel == this

val SHOULD_ZOOM_REVERSE = SystemUtils.IS_OS_MAC_OSX

var shouldBeUndecorated by mutableStateOf(SystemUtils.IS_OS_WINDOWS)
    private set
@Composable
fun FrameWindowScope.initWindowDecoration() {
    if (SystemUtils.IS_OS_WINDOWS) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val titleColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        val textColor = MaterialTheme.colorScheme.onSurface.compositeOver(titleColor)
        window.background = java.awt.Color(0, 0, 0, 255)

        LaunchedEffect(window.windowHandle) {
            try {
                windowsDecorateWindow(window.windowHandle)
            } catch (e: Throwable) {
                shouldBeUndecorated = true
                e.printStackTrace()
            }
        }

        LaunchedEffect(window.windowHandle, titleColor, outlineColor, textColor) {
            try {
                windowsSetWindowColor(window.windowHandle, titleColor)
                windowsSetWindowColor(window.windowHandle, outlineColor, WindowColorType.BORDER)
                windowsSetWindowColor(window.windowHandle, textColor, WindowColorType.TEXT)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
