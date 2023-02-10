package com.eimsound.daw.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.data.midi.NoteMessage
import com.eimsound.audioprocessor.data.midi.NoteMessageList

@Composable
fun MidiView(list: List<NoteMessage>, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    if (list is NoteMessageList) list.read()
    Canvas(modifier.fillMaxSize()) {
        val trackHeightPx = size.height
        val height = (trackHeightPx / 128).coerceAtLeast(1F)
        val noteWidth = size.width / list.maxOf { it.time + it.duration }
        list.fastForEach {
            val y = trackHeightPx - trackHeightPx / 128 * it.note
            drawRect(color, Offset(noteWidth * it.time, y), Size(noteWidth * it.duration, height))
        }
    }
}
