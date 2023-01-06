package cn.apisium.eim.window.panels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.processor.Bus
import cn.apisium.eim.api.processor.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.*
import cn.apisium.eim.components.silder.DefaultTrack
import cn.apisium.eim.components.silder.Slider
import cn.apisium.eim.utils.*

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
private fun MixerTrack(track: Track, index: String, depth: Int = 0, renderChildren: Boolean = true) {
    Row(Modifier.padding(7.dp, 14.dp, 7.dp, 14.dp)
        .shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(((depth + 2) * 2).dp))
        .clip(MaterialTheme.shapes.medium)) {
        val trackColor = if (track is Bus) MaterialTheme.colorScheme.primary else track.color
        val isSelected = EchoInMirror.selectedTrack == track
        var curModifier = Modifier.width(80.dp)
        if (isSelected) curModifier = curModifier.border(1.dp, trackColor, RoundedCornerShape(
            CornerSize(0.dp), CornerSize(0.dp), MaterialTheme.shapes.medium.bottomEnd, MaterialTheme.shapes.medium.bottomStart)
        )

        Layout({
            Column(curModifier.shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
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
                        IconButton({ track.isMute = !track.isMute }, Modifier.size(20.dp)) {
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
                        Gap(18)
                        Level(track.levelMeter, Modifier.height(136.dp))
                    }
                }

                Column {
                    Divider(Modifier.padding(horizontal = 8.dp))
                    track.preProcessorsChain.forEach {
                        MixerProcessorButton(it.name, true, onClick = it::onClick)
                        Divider(Modifier.padding(horizontal = 8.dp))
                    }
                    MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
                    Divider(Modifier.padding(horizontal = 6.dp), 2.dp, MaterialTheme.colorScheme.primary)
                    track.postProcessorsChain.forEach {
                        MixerProcessorButton(it.name, true, onClick = it::onClick)
                        Divider(Modifier.padding(horizontal = 8.dp))
                    }
                    MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
                }
            }
            if (renderChildren && track.subTracks.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 7.dp)) {
                    track.subTracks.forEachIndexed { i, it ->
                        key(it) {
                            MixerTrack(it, "$index.${i + 1}", depth + 1)
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
    override fun icon() {
        Icon(Icons.Default.Tune, "Mixer")
    }

    @Composable
    override fun content() {
        Scrollable {
            Row {
                val bus = EchoInMirror.bus!!
                MixerTrack(bus, "0", renderChildren = false)
                bus.subTracks.forEachIndexed { i, it ->
                    key(it) {
                        MixerTrack(it, (i + 1).toString())
                    }
                }
            }
        }
    }
}
