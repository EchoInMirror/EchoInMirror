package cn.apisium.eim.utils

import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt

val OBJECT_MAPPER = jacksonObjectMapper()
var CLIPBOARD_MANAGER: ClipboardManager? = null

fun lerp(start: Float, stop: Float, fraction: Float) = (1 - fraction) * start + fraction * stop
fun mapValue(value: Float, start1: Float, stop1: Float) = ((value - start1) / (stop1 - start1)).coerceIn(0f, 1f)

fun randomUUID() = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

data class Border(val strokeWidth: Dp, val color: Color, val offset: Dp = 0.dp)

@Suppress("unused")
@Composable
fun getSurfaceColor(elevation: Dp) = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + elevation)

@Stable
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

fun getSampleBits(bits: Int) = (1 shl (8 * bits - 1)) - 1

val PointerEvent.x get() = if (changes.isEmpty()) 0f else changes[0].position.x
val PointerEvent.y get() = if (changes.isEmpty()) 0f else changes[0].position.y

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastAll(predicate: (T) -> Boolean): Boolean {
    contract { callsInPlace(predicate) }
    fastForEach { if (!predicate(it)) return false }
    return true
}

fun Int.fitInUnit(unit: Int) = this / unit * unit

fun Float.fitInUnit(unit: Int) = (this / unit).roundToInt() * unit

@Suppress("NOTHING_TO_INLINE")
inline operator fun String.rem(other: Any?) = format(other)

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onClick(enabled: Boolean = true, onClickLabel: String? = null,
                       role: Role? = null, onClick: () -> Unit = { }) =
    (if (enabled) pointerHoverIcon(PointerIconDefaults.Hand) else this).clickable(enabled, onClickLabel, role, onClick)
