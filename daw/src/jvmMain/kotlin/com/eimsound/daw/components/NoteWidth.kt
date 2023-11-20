package com.eimsound.daw.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.components.gestures.onZoom
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.dawutils.SHOULD_SCROLL_REVERSE
import com.eimsound.daw.dawutils.openMaxValue
import com.eimsound.daw.utils.isCrossPlatformAltPressed
import com.eimsound.daw.utils.isCrossPlatformCtrlPressed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val NOTE_WIDTH_RANGE = 0.02f..5f
private val NOTE_WIDTH_SLIDER_RANGE = (NOTE_WIDTH_RANGE.start / 0.4F)..(NOTE_WIDTH_RANGE.endInclusive / 0.4F)

private fun calcZoom(
    density: Density, coroutineScope: CoroutineScope, delta: Float, scale: Float, x: Float,
    noteWidth: MutableState<Dp>, horizontalScrollState: ScrollState
) {
    density.apply {
        val value = noteWidth.value.value
        val newValue = (if (if (SHOULD_SCROLL_REVERSE) delta < 0 else delta > 0) value * scale else value / scale)
            .coerceIn(NOTE_WIDTH_RANGE)
        val oldX = (x + horizontalScrollState.value) / noteWidth.value.toPx()
        if (newValue != noteWidth.value.value) {
            horizontalScrollState.openMaxValue =
                (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).toPx().toInt()
            noteWidth.value = newValue.dp
            coroutineScope.launch {
                val noteWidthPx = noteWidth.value.toPx()
                horizontalScrollState.scrollBy(
                    (oldX - (x + horizontalScrollState.value) / noteWidthPx) * noteWidthPx
                )
            }
        }
    }
}
fun Modifier.scalableNoteWidth(noteWidth: MutableState<Dp>, horizontalScrollState: ScrollState) = composed {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    onZoom(noteWidth) { zoom, position ->
        calcZoom(density, coroutineScope, zoom, 1.015F, position.x, noteWidth, horizontalScrollState)
    }
}

fun Density.calcScroll(event: PointerEvent, noteWidth: MutableState<Dp>, horizontalScrollState: ScrollState,
                       coroutineScope: CoroutineScope, onVerticalScroll: (PointerInputChange) -> Unit) {
    if (event.keyboardModifiers.isCrossPlatformCtrlPressed) {
        val change = event.changes[0]
        if (event.keyboardModifiers.isCrossPlatformAltPressed) onVerticalScroll(change)
        else {
            calcZoom(this, coroutineScope, change.scrollDelta.y, 1.2F,
                change.position.x, noteWidth, horizontalScrollState)
            change.consume()
        }
    } else {
        val x = event.changes[0].scrollDelta.x
        if (x != 0F) coroutineScope.launch { horizontalScrollState.scrollBy(x * density * 5) }
    }
}

@Composable
fun NoteWidthSlider(noteWidth: MutableState<Dp>) {
    Slider(noteWidth.value.value / 0.4f, { noteWidth.value = 0.4.dp * it }, valueRange = NOTE_WIDTH_SLIDER_RANGE)
}
