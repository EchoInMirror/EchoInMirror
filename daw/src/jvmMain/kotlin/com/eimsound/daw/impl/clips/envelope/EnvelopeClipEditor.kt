package com.eimsound.daw.impl.clips.envelope

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.audioprocessor.projectDisplayPPQ
import com.eimsound.daw.actions.GlobalEnvelopeEditorEventHandler
import com.eimsound.daw.api.*
import com.eimsound.daw.api.clips.ClipEditor
import com.eimsound.daw.api.clips.EnvelopeClip
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.components.*
import com.eimsound.daw.dawutils.openMaxValue
import com.eimsound.daw.components.EditorControls
import com.eimsound.daw.utils.range

class EnvelopeClipEditor(private val clip: TrackClip<EnvelopeClip>) : ClipEditor {
    val noteWidth = mutableStateOf(0.4.dp)
    val horizontalScrollState = ScrollState(0).apply {
        openMaxValue = (noteWidth.value * EchoInMirror.currentPosition.projectDisplayPPQ).value.toInt()
    }

    @Composable
    override fun Editor() {
        Row(Modifier.fillMaxSize()) {
            EditorControls(clip, noteWidth) { }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Box {
                    EditorContent()
                    HorizontalScrollbar(
                        rememberScrollbarAdapter(horizontalScrollState),
                        Modifier.align(Alignment.TopStart).fillMaxWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun EditorContent() {
        Column(Modifier.fillMaxSize()) {
            val range = remember(clip.time, clip.duration) { clip.time..(clip.time + clip.duration) }
            Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, range, 0.dp, EchoInMirror.editUnit,
                EchoInMirror.currentPosition.oneBarPPQ,
                { EchoInMirror.currentPosition.timeInPPQ = it }
            ) {
                clip.time = it.first
                clip.duration = it.range
                clip.track?.clips?.update()
            }
            var contentWidth by remember { mutableStateOf(0.dp) }
            Box(Modifier.fillMaxSize().onGloballyPositioned { contentWidth = it.size.width.dp }
                .scrollable(horizontalScrollState, Orientation.Horizontal, reverseDirection = true)
            ) {
                EchoInMirror.currentPosition.apply {
                    EditorGrid(noteWidth, horizontalScrollState, range, ppq, timeSigDenominator, timeSigNumerator)
                }
                val ctrl = clip.clip.controllers.firstOrNull()?.parameter
                remember(clip, ctrl) {
                    if (ctrl == null) return@remember null
                    EnvelopeEditor(
                        clip.clip.envelope, ctrl.range, ctrl.initialValue, ctrl.isFloat,
                        horizontalScrollState, GlobalEnvelopeEditorEventHandler
                    )
                }?.Editor(
                    clip.start - clip.time + with (LocalDensity.current) { horizontalScrollState.value / noteWidth.value.toPx() },
                    clip.track?.color ?: MaterialTheme.colorScheme.primary,
                    noteWidth, editUnit = EchoInMirror.editUnit,
                    clipStartTime = clip.time
                )
                Box {
                    PlayHead(noteWidth, horizontalScrollState,
                        (EchoInMirror.currentPosition.ppqPosition * EchoInMirror.currentPosition.ppq).toFloat(),
                        contentWidth)
                }
            }
        }
    }
}