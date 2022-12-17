package cn.apisium.eim.window

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.*
import cn.apisium.eim.components.silder.DefaultTrack
import cn.apisium.eim.components.silder.Slider
import cn.apisium.eim.utils.toOnSurfaceColor

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
private fun MixerTrack(track: Track, index: Int, height: MutableState<Dp>?, modifier: Modifier = Modifier, isRound: Boolean = false, renderChildren: Boolean = true) {
    Row(if (isRound) modifier.shadow(1.dp, MaterialTheme.shapes.medium, clip = false)
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).clip(MaterialTheme.shapes.medium)
    else modifier) {
        Column(Modifier.width(80.dp)) {
            Row(Modifier.background(track.color).clickable { EchoInMirror.selectedTrack = track }.padding(vertical = 2.5.dp)) {
                val color = track.color.toOnSurfaceColor()
                Text(
                    index.toString(),
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
                    thumbSize = DpSize(14.dp, 14.dp),
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
                    MixerProcessorButton(it.name, true) { }
                    Divider(Modifier.padding(horizontal = 8.dp))
                }
                MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
                Divider(Modifier.padding(horizontal = 6.dp), 2.dp, MaterialTheme.colorScheme.primary)
                track.postProcessorsChain.forEach {
                    MixerProcessorButton(it.name, true) { }
                    Divider(Modifier.padding(horizontal = 8.dp))
                }
                MixerProcessorButton("...", fontWeight = FontWeight.Bold) { }
            }
        }

        if (renderChildren && track.subTracks.isNotEmpty()) {
            track.subTracks.forEachIndexed { i, it ->
                key(it) {
                    if (height != null) Spacer(Modifier.width(1.dp).height(height.value - 8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant))
                    MixerTrack(it, i + 1, height = height)
                }
            }
        }
    }
}

class Mixer: Panel {
    override val name = "混音台"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun icon() {
        Icon(Icons.Default.Tune, "Mixer")
    }

    @Preview
    @Composable
    override fun content() {
        Scrollable {
            Row(Modifier.padding(14.dp)) {
                MixerTrack(EchoInMirror.bus, 0, null, Modifier, true, renderChildren = false)
                val localDensity = LocalDensity.current
                EchoInMirror.bus.subTracks.forEachIndexed { i, it ->
                    key(it) {
                        Gap(8)
                        val state = remember { mutableStateOf(0.dp) }
                        MixerTrack(it, i + 1, state, Modifier.onGloballyPositioned {
                            with (localDensity) { state.value = it.size.height.toDp()  }
                        }, true)
                    }
                }
            }
        }
    }
}
