package com.eimsound.daw.components.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FPSMeasurer(modifier: Modifier) {
    var fps by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        var fpsCount = 0
        var lasUpdate = 0L
        while (true) {
            withFrameMillis {
                if (++fpsCount == 5) {
                    fps = (fpsCount * 1000 / (it - lasUpdate)).toInt()
                    fpsCount = 0
                    lasUpdate = it
                }
            }
        }
    }

    Text("FPS: $fps", modifier, color = MaterialTheme.colorScheme.absoluteError, fontWeight = FontWeight.Bold)
}
