package com.eimsound.daw.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eimsound.daw.EchoInMirror
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.SystemUtils
import org.ocpsoft.prettytime.PrettyTime
import java.awt.Component
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.JFileChooser

private val ZERO_DATE = Date(0)
val OBJECT_MAPPER = jacksonObjectMapper()
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

val PointerEvent.x get() = if (changes.isEmpty()) 0f else changes[0].position.x
val PointerEvent.y get() = if (changes.isEmpty()) 0f else changes[0].position.y

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
fun Modifier.clickableWithIcon(enabled: Boolean = true, onClickLabel: String? = null,
                       role: Role? = null, onLongClick: (() -> Unit)? = null,
                     onDoubleClick: (() -> Unit)? = null, onClick: () -> Unit = { }): Modifier {
    val modifier = if (enabled) pointerHoverIcon(PointerIconDefaults.Hand) else this
    return if (onLongClick != null || onDoubleClick != null) modifier.clickable(enabled, onClickLabel, role) { }
        .combinedClickable(onLongClick = onLongClick, onDoubleClick = onDoubleClick, onClick = onClick)
    else modifier.clickable(enabled, onClickLabel, role, onClick)
}

var ScrollState.openMaxValue
    get() = maxValue
    @Suppress("INVISIBLE_SETTER")
    set(value) { maxValue = value }

@Suppress("INVISIBLE_MEMBER")
val CurrentWindow @Composable get() = androidx.compose.ui.window.LocalWindow

fun openFolderBrowser(parent: Component? = null): File? {
    val fileChooser = JFileChooser()
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.showOpenDialog(parent)
    return fileChooser.selectedFile
}

fun formatDuration(time: Long): String = DURATION_PRETTIER.formatDuration(Date(time)).ifEmpty { DURATION_PRETTIER.format(ZERO_DATE) }

fun openInExplorer(file: File) = Desktop.getDesktop().open(file)
fun openInBrowser(uri: URI) = Desktop.getDesktop().browse(uri)
fun selectInExplorer(file: File) {
    Desktop.getDesktop().apply {
        if (isSupported(Desktop.Action.BROWSE_FILE_DIR)) browseFileDirectory(file)
        else if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec("explorer.exe /select,\"${file.absolutePath}\"")
    }
}

fun randomColor() = com.eimsound.daw.components.utils.randomColor(!EchoInMirror.windowManager.isDarkTheme)
