package com.eimsound.daw.impl.clips.audio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.daw.api.clips.AudioClip
import com.eimsound.daw.api.clips.TrackClip
import com.eimsound.daw.components.*
import com.eimsound.dsp.timestretcher.TimeStretcherManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
internal fun EditorControls(clip: TrackClip<AudioClip>) {
    Column(Modifier.widthIn(220.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val c = clip.clip
        Row(verticalAlignment = Alignment.CenterVertically) {
            val snackbarProvider = LocalSnackbarProvider.current
            CustomOutlinedTextField(
                if (c.bpm <= 0) "" else "%.2f".format(c.bpm), { c.bpm = it.toFloatOrNull() ?: 0F },
                Modifier.weight(1F).height(40.dp),
                label = { Text("文件速度") },
                singleLine = true
            )
            TextButton(onClick = {
                val c2 = clip.clip as? AudioClipImpl ?: return@TextButton
                GlobalScope.launch {
                    val bpm = c2.detectBPM(snackbarProvider)
                    if (bpm > 0) c2.bpm = bpm.toFloat()
                }
            }) {
                Text("检测速度")
            }
        }
        val timeStretchers = TimeStretcherManager.timeStretchers
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomOutlinedTextField(
                "%.2f".format(c.speedRatio),
                {
                    c.timeStretcher.ifEmpty { c.timeStretcher = timeStretchers.firstOrNull() ?: "" }
                    c.speedRatio = it.toFloatOrNull() ?: 1F
                },
                Modifier.height(40.dp).weight(1F), label = { Text("变速") },
                singleLine = true
            )
            Gap(8)
            CustomOutlinedTextField(
                "%.2f".format(c.semitones),
                {
                    c.timeStretcher.ifEmpty { c.timeStretcher = timeStretchers.firstOrNull() ?: "" }
                    c.semitones = it.toFloatOrNull() ?: 0F
                },
                Modifier.height(40.dp).weight(1F), label = { Text("变调") },
                singleLine = true
            )
            TextButton(onClick = { }) {
                Text("变调到...")
            }
        }
        OutlinedDropdownSelector(
            { c.timeStretcher = it },
            timeStretchers, c.timeStretcher.ifEmpty { timeStretchers.first() },
            Modifier.height(40.dp).fillMaxWidth(), label = "变速算法"
        )
    }
}
