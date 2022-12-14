package cn.apisium.eim.impl.clips.midi.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteDisabledAction
import cn.apisium.eim.actions.doNoteVelocityAction
import cn.apisium.eim.api.MidiClip
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.components.*
import cn.apisium.eim.components.silder.Slider
import cn.apisium.eim.utils.toOnSurfaceColor
import kotlin.math.roundToInt

private fun dfsTrackIndex(track: Track, target: Track, index: String): String? {
    if (track == target) return index
    track.subTracks.forEachIndexed { i, child ->
        val res = dfsTrackIndex(child, target, "$index.${i + 1}")
        if (res != null) return res
    }
    return null
}

@Composable
private fun TrackItem(track: Track, index: String, depth: Int = 0) {
    val isSelected = EchoInMirror.selectedClip?.clip == track
    MenuItem(isSelected || backingTracks.contains(track), {
        if (!backingTracks.remove(track)) backingTracks.add(track)
    }, minHeight = 28.dp, padding = PaddingValues(), modifier = Modifier.height(IntrinsicSize.Min)) {
        Spacer(Modifier.width(6.dp * depth))
        Spacer(Modifier.width(6.dp).fillMaxHeight().background(track.color))
        Text(index + " " + track.name, Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp), maxLines = 1,
            style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
    track.subTracks.forEachIndexed { i, it -> TrackItem(it, index + "." + (i + 1), depth + 1) }
}

@Composable
internal fun EditorControls(clip: MidiClip) {
    val track = EchoInMirror.selectedTrack
    FloatingDialog({ size, _ ->
        Surface(Modifier.width(IntrinsicSize.Max).widthIn(min = size.width.dp), shape = MaterialTheme.shapes.extraSmall,
            tonalElevation = 5.dp, shadowElevation = 5.dp) {
            val stateVertical = rememberScrollState(0)
            Column(Modifier.verticalScroll(stateVertical).padding(vertical = 4.dp)) {
                EchoInMirror.bus!!.subTracks.forEachIndexed { index, it -> TrackItem(it, (index + 1).toString()) }
            }
        }
    }) {
        val color by animateColorAsState(track?.color ?: MaterialTheme.colorScheme.surface, tween(100))
        Surface(Modifier.fillMaxWidth().height(TIMELINE_HEIGHT), shadowElevation = 2.dp, tonalElevation = 4.dp, color = color) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val textColor by animateColorAsState(color.toOnSurfaceColor(), tween(80))
                Text(
                    remember(track) {
                        if (track == null) null else dfsTrackIndex(EchoInMirror.bus!!, track, "")?.trimStart('.')
                    } ?: "?", Modifier.padding(horizontal = 8.dp),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                )
                Text(track?.name ?: "?????????", Modifier.weight(1f), textColor, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp), textColor)
            }
        }
    }
    Column(Modifier.padding(10.dp)) {
        Slider(noteWidth.value.value / 0.4f, { noteWidth.value = 0.4.dp * it }, valueRange = 0.15f..8f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("????????????", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Checkbox(playOnEdit, { playOnEdit = !playOnEdit })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            clip.notes.read()
            var delta by remember { mutableStateOf(0) }
            val cur = currentSelectedNote
            val velocity = if (selectedNotes.isEmpty()) defaultVelocity else (cur ?: selectedNotes.first()).velocity
            val trueValue = velocity + (if (selectedNotes.isEmpty()) 0 else delta)
            CustomTextField(trueValue.toString(), { str ->
                val v = str.toIntOrNull()?.coerceIn(0, 127) ?: return@CustomTextField
                if (selectedNotes.isEmpty()) defaultVelocity = v
                else clip.doNoteVelocityAction(selectedNotes.toTypedArray(), v - velocity)
            }, Modifier.width(60.dp).padding(end = 10.dp), label = { Text("??????") })
            Slider(trueValue.toFloat() / 127,
                {
                    if (selectedNotes.isEmpty()) defaultVelocity = (it * 127).roundToInt()
                    else delta = (it * 127).roundToInt() - velocity
                }, Modifier.weight(1f), onValueChangeFinished = {
                    if (selectedNotes.isNotEmpty()) clip.doNoteVelocityAction(selectedNotes.toTypedArray(), delta)
                    delta = 0
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            clip.notes.read()
            Text("??????", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            val cur = currentSelectedNote ?: selectedNotes.firstOrNull()
            val curState = cur?.disabled ?: true
            Checkbox(curState, {
                if (cur == null) return@Checkbox
                clip.doNoteDisabledAction(selectedNotes.toTypedArray(), !curState)
            })
        }
    }
}
