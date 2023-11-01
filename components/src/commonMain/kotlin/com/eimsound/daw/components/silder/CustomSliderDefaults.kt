package com.eimsound.daw.components.silder

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun DefaultTrack(
    modifier: Modifier,
    progress: Float,
    @Suppress("UNUSED_PARAMETER") interactionSource: MutableInteractionSource,
    tickFractions: List<Float>,
    isVertical: Boolean = false,
    colorProgress: Color = MaterialTheme.colorScheme.primary,
    colorTrack: Color = colorProgress.copy(alpha = 0.3f),
    colorTickTrack: Color = colorProgress,
    colorTickProgress: Color = colorTrack,
    stroke: Dp = 4.dp,
    startPoint: Float = 0f
) {
    Spacer(
        (if (isVertical) Modifier.then(modifier).width(stroke) else Modifier.then(modifier).height(stroke)).drawWithCache {
            val center = size.center
            val sliderLeft = if (isVertical) Offset(center.x, size.height) else Offset(0f, center.y)
            val sliderRight = if (isVertical) Offset(center.x, 0f) else Offset(size.width, center.y)
            val stroke0 = if (isVertical) size.width else size.height

            val sliderValueEnd = if (isVertical) Offset(center.x, (sliderLeft.y - sliderRight.y) * progress)
            else Offset(sliderLeft.x + (sliderRight.x - sliderLeft.x) * progress, center.y)

            val sliderValueStart = if (isVertical) Offset(center.x, startPoint * size.height) else Offset(startPoint * size.width, center.y)

            val afterFractions = mutableListOf<Offset>()
            val beforeFractions = mutableListOf<Offset>()
            val progress0 = if (isVertical) 1 - progress else progress
            tickFractions.forEach {
                (if (it > progress0) afterFractions else beforeFractions).add(
                    if (isVertical) Offset(center.x, lerp(sliderLeft, sliderRight, it).y)
                    else Offset(lerp(sliderLeft, sliderRight, it).x, center.y)
                )
            }

            onDrawBehind {
                drawLine(
                    colorTrack,
                    sliderLeft,
                    sliderRight,
                    stroke0,
                    StrokeCap.Round
                )

                drawLine(
                    colorProgress,
                    sliderValueStart,
                    sliderValueEnd,
                    stroke0,
                    StrokeCap.Round
                )

                if (afterFractions.isNotEmpty()) drawPoints(afterFractions, PointMode.Points, colorTickTrack,
                    stroke0, StrokeCap.Round)
                if (beforeFractions.isNotEmpty()) drawPoints(beforeFractions, PointMode.Points, colorTickProgress,
                    stroke0, StrokeCap.Round)
            }
        }
    )
}

@Composable
fun MutableInteractionSource.ListenOnPressed(onPressChange: (Boolean) -> Unit) {
    val interactionSource = this

    val onPressChangeState = rememberUpdatedState(onPressChange)

    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }
    onPressChangeState.value(interactions.isNotEmpty())
}

/**
 * @param scaleOnPress - if more than 1f uses animation of scale otherwise no animation on press
 */
@Composable
fun DefaultThumb(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    color: Color = MaterialTheme.colorScheme.primary,
    scaleOnPress: Float = 1.3f,
    animationSpec: AnimationSpec<Float> = SpringSpec(0.3f)
) {

    var isPressed by remember { mutableStateOf(false) }

    if (scaleOnPress > 1f) {
        interactionSource.ListenOnPressed { isPressed = it }
    }

    val scale: Float by animateFloatAsState(
        if (isPressed) scaleOnPress else 1f,
        animationSpec = animationSpec
    )

    Spacer(modifier
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .background(color, CircleShape)
    )
}


internal val DEFAULT_THUMB_SIZE = DpSize(14.dp, 14.dp)
