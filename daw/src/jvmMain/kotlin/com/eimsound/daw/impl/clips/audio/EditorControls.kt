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
    Column(Modifier.widthIn(200.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val snackbarProvider = LocalSnackbarProvider.current
            CustomOutlinedTextField("", { }, Modifier.weight(1F).height(40.dp), label = { Text("文件速度") })
            TextButton(onClick = {
                val c = clip.clip as? AudioClipImpl ?: return@TextButton
                GlobalScope.launch { c.detectBPM(snackbarProvider) }
            }) {
                Text("检测速度")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomOutlinedTextField("1.0", { }, Modifier.height(40.dp).weight(1F), label = { Text("变速") })
            Gap(8)
            CustomOutlinedTextField("0", { }, Modifier.height(40.dp).weight(1F), label = { Text("变调") })
            TextButton(onClick = { }) {
                Text("变调到...")
            }
        }
        OutlinedDropdownSelector({}, TimeStretcherManager.timeStretchers, "", Modifier.height(40.dp).fillMaxWidth(), label = "变速算法")
    }
}
