package com.eimsound.daw.dawutils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.eimsound.daw.api.EditorTool
import com.eimsound.daw.components.icons.Eraser
import org.apache.commons.lang3.SystemUtils
import java.awt.Point
import java.awt.Toolkit
import kotlin.math.roundToInt

val EDITOR_TOOL_ICONS = arrayOf(
    Icons.Outlined.NearMe, Icons.Outlined.Edit, Eraser, Icons.Outlined.VolumeOff,
    Icons.Outlined.ContentCut
)

val CURSOR_ICONS = mutableStateListOf<PointerIcon?>(null, null, null, null)

private val TOOLS_HOT_SPOTS = arrayOf(
    arrayOf(0F, 1F), arrayOf(0F, 1F), arrayOf(0.5F, 0.5F), arrayOf(1F, 0.5F)
)

@Composable
fun InitEditorTools() {
    val density = LocalDensity.current
    val toolkit = Toolkit.getDefaultToolkit()
    val d = toolkit.getBestCursorSize((10 * density.density).roundToInt(), (10 * density.density).roundToInt())
    val padding = if (SystemUtils.IS_OS_WINDOWS) 5 * density.density else 1F
    val size = Size(d.width.toFloat() - padding * 2, d.height.toFloat() - padding * 2)
    val borderColor = ColorFilter.tint(Color.White)
    EDITOR_TOOL_ICONS.run {
        forEachIndexed { i, it ->
            if (i == 0) return@forEachIndexed
            val p = rememberVectorPainter(it)
            LaunchedEffect(p) {
                val img = ImageBitmap(d.width, d.height)
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(img), size) {
                    with(p) {
                        repeat(3) { i -> repeat(3) { j ->
                            translate(padding + i - 1, padding + j - 1) {
                                draw(size, colorFilter = borderColor)
                            }
                        } }
                        translate(padding, padding) { draw(size) }
                    }
                }
                val hotSpot = TOOLS_HOT_SPOTS[i - 1]
                val icon = PointerIcon(toolkit.createCustomCursor(
                    img.toAwtImage(),
                    Point((hotSpot[0] * d.width * 0.99999F).toInt(), (hotSpot[1] * d.height * 0.99999F).toInt()),
                    it.name
                ))
                CURSOR_ICONS[i - 1] = icon
                EditorTool.entries[i].pointerIcon = icon
            }
        }
    }
}

fun Modifier.editorToolHoverIcon(tool: EditorTool) = if (tool.ordinal == 0) this else pointerHoverIcon(tool.pointerIcon)
