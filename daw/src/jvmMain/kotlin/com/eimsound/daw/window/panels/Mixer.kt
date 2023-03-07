package com.eimsound.daw.window.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.silder.DefaultTrack
import com.eimsound.daw.components.silder.Slider
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.components.utils.toOnSurfaceColor

@Composable
fun MixerProcessorButton(title: String, useMarquee: Boolean = false, fontStyle: FontStyle? = null,
                         fontWeight: FontWeight? = null, onClick: () -> Unit) {
    TextButton(onClick, Modifier.height(30.dp).fillMaxWidth()) {
        if (useMarquee) Marquee { Text(title, Modifier.fillMaxWidth(), fontSize = MaterialTheme.typography.labelMedium.fontSize,
            maxLines = 1, lineHeight = 7.sp, textAlign = TextAlign.Center, fontStyle = fontStyle, fontWeight = fontWeight) }
        else Text(title, fontSize = MaterialTheme.typography.labelMedium.fontSize, maxLines = 1, lineHeight = 7.sp,
            fontStyle = fontStyle, fontWeight = fontWeight)
    }
}

@Composable
private fun MixerTrack(track: Track, index: String, containerColor: Color = MaterialTheme.colorScheme.surface,
                       depth: Int = 0, renderChildren: Boolean = true) {
    Row(Modifier.padding(7.dp, 14.dp, 7.dp, 14.dp)
        .shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(((depth + 2) * 2).dp))
        .clip(MaterialTheme.shapes.medium)) {
        val trackColor = if (track is Bus) MaterialTheme.colorScheme.primary else track.color
        val isSelected = EchoInMirror.selectedTrack == track
        var curModifier = Modifier.width(80.dp)
        if (isSelected) curModifier = curModifier.border(1.dp, trackColor, MaterialTheme.shapes.medium)
        Layout({
            Column(curModifier.shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
                .background(containerColor, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)) {
                Row(Modifier.background(trackColor).height(24.dp)
                    .clickableWithIcon { if (track != EchoInMirror.bus) EchoInMirror.selectedTrack = track }
                    .padding(vertical = 2.5.dp).zIndex(2f)) {
                    val color = trackColor.toOnSurfaceColor()
                    Text(
                        index,
                        Modifier.padding(start = 5.dp, end = 3.dp),
                        color = color,
                        fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        lineHeight = 18.0.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Marquee {
                        Text(
                            track.name,
                            color = color,
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            lineHeight = 18.0.sp,
                        )
                    }
                }

                Column(Modifier.padding(4.dp)) {
                    Slider(
                        track.pan,
                        { track.pan = it },
                        valueRange = -1f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        track = { modifier, progress, interactionSource, tickFractions, enabled, isVertical ->
                            DefaultTrack(modifier, progress, interactionSource, tickFractions, enabled, isVertical, startPoint = 0.5f)
                        }
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton({ track.isMute = !track.isMute }, 20.dp) {
                            if (track.isMute) Icon(Icons.Default.VolumeOff, null, tint = MaterialTheme.colorScheme.error)
                            else Icon(Icons.Default.VolumeUp, null)
                        }
                        Text(
                            track.levelMeter.cachedMaxLevelString,
                            Modifier.width(30.dp).padding(start = 2.dp),
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1).sp,
                            lineHeight = 12.sp,
                            maxLines = 1
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        VolumeSlider(track)
                        LevelHints(Modifier.height(136.dp).width(24.dp))
                        Level(track.levelMeter, Modifier.height(136.dp))
                    }
                }

                Column {
                    Divider(Modifier.padding(horizontal = 8.dp))
                    track.preProcessorsChain.fastForEach {
                        MixerProcessorButton(it.name, true, onClick = it::onClick)
                        Divider(Modifier.padding(horizontal = 8.dp))
                    }
                    MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
                    Divider(Modifier.padding(horizontal = 6.dp), 2.dp, MaterialTheme.colorScheme.primary)
                    track.postProcessorsChain.fastForEach {
                        MixerProcessorButton(it.name, true, onClick = it::onClick)
                        Divider(Modifier.padding(horizontal = 8.dp))
                    }
                    MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
                }
            }
            if (renderChildren && track.subTracks.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 7.dp)) {
                    track.subTracks.fastForEachIndexed { i, it ->
                        key(it) {
                            MixerTrack(it, "$index.${i + 1}", containerColor, depth + 1)
                        }
                    }
                }
            }
        }) { measurables, constraints ->
            if (measurables.size > 1) {
                val content = measurables[1].measure(constraints)
                var maxHeight = content.height
                val mixer = measurables[0].measure(constraints.copy(minHeight = maxHeight))
                if (mixer.height > maxHeight) maxHeight = mixer.height
                layout(content.width + mixer.width, maxHeight) {
                    mixer.place(0, 0)
                    content.place(mixer.width, 0)
                }
            } else {
                val mixer = measurables[0].measure(constraints)
                layout(mixer.width, mixer.height) {
                    mixer.place(0, 0)
                }
            }
        }
    }
}

object Mixer: Panel {
    override val name = "混音台"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun Icon() {
        Icon(Icons.Default.Tune, name)
    }

    @Composable
    override fun Content() {
        Scrollable {
            Row {
                val bus = EchoInMirror.bus!!
                val trackColor = if (EchoInMirror.windowManager.isDarkTheme)
                    MaterialTheme.colorScheme.surfaceColorAtElevation(20.dp) else MaterialTheme.colorScheme.surface
                MixerTrack(bus, "0", trackColor, renderChildren = false)
                bus.subTracks.fastForEachIndexed { i, it ->
                    key(it) {
                        MixerTrack(it, (i + 1).toString(), trackColor)
                    }
                }
            }
        }
    }
}
