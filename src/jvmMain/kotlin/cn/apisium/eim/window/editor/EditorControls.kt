package cn.apisium.eim.window.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.actions.doNoteVelocityAction
import cn.apisium.eim.api.Track
import cn.apisium.eim.components.CustomTextField
import cn.apisium.eim.components.Marquee
import cn.apisium.eim.components.TIMELINE_HEIGHT
import cn.apisium.eim.components.silder.Slider
import kotlin.math.roundToInt

private fun dfsTrackIndex(track: Track, target: Track, index: String): String? {
    if (track == target) return index
    track.subTracks.forEachIndexed { i, child ->
        val res = dfsTrackIndex(child, target, "$index-${i + 1}")
        if (res != null) return res
    }
    return null
}

@Composable
internal fun EditorControls() {
    Surface(Modifier.fillMaxWidth().height(TIMELINE_HEIGHT).clickable {  }, shadowElevation = 2.dp, tonalElevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val track = EchoInMirror.selectedTrack
            Text(
                remember(track) { if (track == null) null else dfsTrackIndex(EchoInMirror.bus, track, "")?.trimStart('-') } ?: "?",
                Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.labelLarge.fontSize
            )
            Marquee { Text(track?.name ?: "未选择", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge) }
        }
    }
    Column(Modifier.padding(10.dp)) {
        Slider(noteWidth.value / 0.4f, { noteWidth = 0.4.dp * it }, valueRange = 0.15f..8f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("试听音符", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Checkbox(playOnEdit, { playOnEdit = !playOnEdit })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val track = EchoInMirror.selectedTrack
            track?.notes?.read()
            var delta by remember { mutableStateOf(0) }
            currentSelectedNote
            val velocity = if (selectedNotes.isEmpty()) defaultVelocity else selectedNotes.first().velocity
            val trueValue = velocity + (if (selectedNotes.isEmpty()) 0 else delta)
            CustomTextField(trueValue.toString(), { str ->
                val v = str.toIntOrNull()?.coerceIn(0, 127) ?: return@CustomTextField
                if (selectedNotes.isEmpty()) defaultVelocity = v else track?.doNoteVelocityAction(selectedNotes.toTypedArray(), v - velocity)
            }, Modifier.width(60.dp).padding(end = 10.dp), label = { Text("力度") })
            Slider(trueValue.toFloat() / 127,
                {
                    if (selectedNotes.isEmpty()) defaultVelocity = (it * 127).roundToInt()
                    else delta = (it * 127).roundToInt() - velocity
                }, onValueChangeFinished = {
                    if (selectedNotes.isNotEmpty()) track?.doNoteVelocityAction(selectedNotes.toTypedArray(), delta)
                    delta = 0
                },
                modifier = Modifier.weight(1f))
        }
    }
}
