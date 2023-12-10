package com.eimsound.daw.components

// From https://github.com/godaddy/compose-color-picker

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eimsound.daw.components.utils.HsvColor
import com.eimsound.daw.components.utils.randomColor
import java.lang.Double.min
import kotlin.math.*

enum class ColorHarmonyMode {
    NONE,
    COMPLEMENTARY,
    ANALOGOUS,
    SPLIT_COMPLEMENTARY,
    TRIADIC,
    TETRADIC,
    MONOCHROMATIC,
    SHADES;
}

@Composable
fun ColorPicker(
    color: HsvColor,
    modifier: Modifier = Modifier,
    harmonyMode: ColorHarmonyMode = ColorHarmonyMode.NONE,
    onColorChanged: (HsvColor) -> Unit
) {
    BoxWithConstraints(modifier) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            val updatedColor by rememberUpdatedState(color)
            val updatedOnValueChanged by rememberUpdatedState(onColorChanged)

            HarmonyColorPickerWithMagnifiers(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f),
                hsvColor = updatedColor,
                onColorChanged = {
                    updatedOnValueChanged(it)
                },
                harmonyMode = harmonyMode
            )

            BrightnessBar(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .weight(0.2f),
                onValueChange = { value ->
                    updatedOnValueChanged(updatedColor.copy(value = value))
                },
                currentColor = updatedColor
            )
        }
    }
}

@Composable
private fun HarmonyColorPickerWithMagnifiers(
    modifier: Modifier = Modifier,
    hsvColor: HsvColor,
    onColorChanged: (HsvColor) -> Unit,
    harmonyMode: ColorHarmonyMode
) {
    val hsvColorUpdated by rememberUpdatedState(hsvColor)
    BoxWithConstraints(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp)
            .wrapContentSize()
            .aspectRatio(1f, matchHeightConstraintsFirst = true)

    ) {
        val updatedOnColorChanged by rememberUpdatedState(onColorChanged)
        val diameterPx by remember(constraints.maxWidth) {
            mutableStateOf(constraints.maxWidth)
        }

        var animateChanges by remember {
            mutableStateOf(false)
        }
        var currentlyChangingInput by remember {
            mutableStateOf(false)
        }

        fun updateColorWheel(newPosition: Offset, animate: Boolean) {
            // Work out if the new position is inside the circle we are drawing, and has a
            // valid color associated to it. If not, keep the current position
            val newColor = colorForPosition(newPosition, IntSize(diameterPx, diameterPx), hsvColorUpdated.value)
            if (newColor != null) {
                animateChanges = animate
                updatedOnColorChanged(newColor)
            }
        }

        val inputModifier = Modifier.pointerInput(diameterPx) {
            awaitEachGesture {
                val down = awaitFirstDown(false)
                currentlyChangingInput = true
                updateColorWheel(down.position, animate = true)
                drag(down.id) { change ->
                    updateColorWheel(change.position, animate = false)
                    if (change.positionChange() != Offset.Zero) change.consume()
                }
                currentlyChangingInput = false
            }
        }

        Box(inputModifier.fillMaxSize()) {
            ColorWheel(hsvColor = hsvColor, diameter = diameterPx)
            HarmonyColorMagnifiers(
                diameterPx,
                hsvColor,
                animateChanges,
                currentlyChangingInput,
                harmonyMode
            )
        }
    }
}

private fun colorForPosition(position: Offset, size: IntSize, value: Float): HsvColor? {
    val centerX: Double = size.width / 2.0
    val centerY: Double = size.height / 2.0
    val radius: Double = min(centerX, centerY)
    val xOffset: Double = position.x - centerX
    val yOffset: Double = position.y - centerY
    val centerOffset = hypot(xOffset, yOffset)
    val rawAngle = atan2(yOffset, xOffset) * 180 / PI
    val centerAngle = (rawAngle + 360.0) % 360.0
    return if (centerOffset <= radius) {
        HsvColor(
            hue = centerAngle.toFloat(),
            saturation = (centerOffset / radius).toFloat(),
            value = value,
            alpha = 1.0f
        )
    } else {
        null
    }
}

@Composable
private fun BrightnessBar(
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
    currentColor: HsvColor
) {
    Slider(
        modifier = modifier,
        value = currentColor.value,
        onValueChange = onValueChange,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            thumbColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun ColorWheel(
    hsvColor: HsvColor,
    diameter: Int
) {
    val saturation = 1.0f
    val value = hsvColor.value

    val radius = diameter / 2f
    val alpha = 1.0f
    val colorSweepGradientBrush = remember(hsvColor.value, diameter) {
        val wheelColors = arrayOf(
            HsvColor(0f, saturation, value, alpha),
            HsvColor(60f, saturation, value, alpha),
            HsvColor(120f, saturation, value, alpha),
            HsvColor(180f, saturation, value, alpha),
            HsvColor(240f, saturation, value, alpha),
            HsvColor(300f, saturation, value, alpha),
            HsvColor(360f, saturation, value, alpha)
        ).map {
            it.toColor()
        }
        Brush.sweepGradient(wheelColors, Offset(radius, radius))
    }
    val saturationGradientBrush = remember(diameter) {
        Brush.radialGradient(
            listOf(Color.White, Color.Transparent),
            Offset(radius, radius),
            radius,
            TileMode.Clamp
        )
    }
    Surface(shape = CircleShape, shadowElevation = 3.dp) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // draw the hue bar
            drawCircle(colorSweepGradientBrush)
            // draw saturation radial overlay
            drawCircle(saturationGradientBrush)
            // account for "brightness/value" slider
            drawCircle(
                hsvColor.copy(
                    hue = 0f,
                    saturation = 0f
                ).toColor(),
                blendMode = BlendMode.Modulate
            )
        }
    }
}

@Composable
internal fun HarmonyColorMagnifiers(
    diameterPx: Int,
    hsvColor: HsvColor,
    animateChanges: Boolean,
    currentlyChangingInput: Boolean,
    harmonyMode: ColorHarmonyMode
) {
    val size = IntSize(diameterPx, diameterPx)
    val position = remember(hsvColor, size) {
        positionForColor(hsvColor, size)
    }

    val positionAnimated = remember {
        Animatable(position, typeConverter = Offset.VectorConverter)
    }
    LaunchedEffect(hsvColor, size, animateChanges) {
        if (!animateChanges) {
            positionAnimated.snapTo(positionForColor(hsvColor, size))
        } else {
            positionAnimated.animateTo(
                positionForColor(hsvColor, size),
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
        }
    }

    val diameterDp = with(LocalDensity.current) {
        diameterPx.toDp()
    }

    val animatedDiameter = animateDpAsState(
        targetValue = if (!currentlyChangingInput) {
            diameterDp * diameterMainColorDragging
        } else {
            diameterDp * diameterMainColor
        }
    )

    hsvColor.getColors(harmonyMode).forEach { color ->
        val positionForColor = remember {
            Animatable(positionForColor(color, size), typeConverter = Offset.VectorConverter)
        }
        LaunchedEffect(color, size, animateChanges) {
            if (!animateChanges) {
                positionForColor.snapTo(positionForColor(color, size))
            } else {
                positionForColor.animateTo(
                    positionForColor(color, size),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                )
            }
        }
        Magnifier(position = positionForColor.value, color = color, diameter = diameterDp * diameterHarmonyColor)
    }
    Magnifier(position = positionAnimated.value, color = hsvColor, diameter = animatedDiameter.value)
}

private fun positionForColor(color: HsvColor, size: IntSize): Offset {
    val radians = color.hue  / 180.0f * PI.toFloat()
    val phi = color.saturation
    val x: Float = ((phi * cos(radians)) + 1) / 2f
    val y: Float = ((phi * sin(radians)) + 1) / 2f
    return Offset(
        x = (x * size.width),
        y = (y * size.height)
    )
}

private const val diameterHarmonyColor = 0.10f
private const val diameterMainColorDragging = 0.18f
private const val diameterMainColor = 0.15f

@Composable
private fun Magnifier(position: Offset, color: HsvColor, diameter: Dp) {
    val offset = with(LocalDensity.current) {
        Modifier.offset(
            position.x.toDp() - diameter / 2,
            // Align with the center of the selection circle
            position.y.toDp() - diameter / 2
        )
    }

    Column(offset.size(width = diameter, height = diameter)) {
        MagnifierSelectionCircle(Modifier.size(diameter), color)
    }
}

/**
 * Selection circle drawn over the currently selected pixel of the color wheel.
 */
@Composable
private fun MagnifierSelectionCircle(modifier: Modifier, color: HsvColor) {
    Surface(
        modifier,
        shape = CircleShape,
        shadowElevation = 4.dp,
        color = color.toColor(),
        border = BorderStroke(2.dp, SolidColor(Color.White)),
        content = {}
    )
}

private val KEY = Any()

fun FloatingLayerProvider.openColorPicker(
    initialColor: Color = randomColor(), onCancel: (() -> Unit)? = null, onChange: (Color) -> Unit
) {
    closeFloatingLayer(KEY)
    openFloatingLayer({
        closeFloatingLayer(KEY)
        onCancel?.invoke()
    }, key = KEY, hasOverlay = true) {
        Dialog {
            var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }
            ColorPicker(currentColor, Modifier.size(200.dp)) {
                currentColor = it
                onChange(it.toColor())
            }
            Row(Modifier.fillMaxWidth().padding(end = 10.dp),
                horizontalArrangement = Arrangement.End) {
                TextButton({
                    closeFloatingLayer(KEY)
                    onCancel?.invoke()
                }) { Text("取消") }
                TextButton({
                    closeFloatingLayer(KEY)
                }) { Text("确认") }
            }
        }
    }
}

