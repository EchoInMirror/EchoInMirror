package cn.apisium.eim.window

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.app.BottomContentScrollable
import cn.apisium.eim.components.Gap
import cn.apisium.eim.components.Level
import cn.apisium.eim.components.VolumeSlider
import cn.apisium.eim.components.Marquee
import cn.apisium.eim.components.silder.DefaultTrack
import cn.apisium.eim.components.silder.Slider
import cn.apisium.eim.utils.toOnSurfaceColor

@Composable
private fun MixerTrack(track: Track, index: Int, isRound: Boolean = false, renderChildren: Boolean = true) {
    Surface(
        shadowElevation = 1.dp,
        shape = if (isRound) MaterialTheme.shapes.medium else RectangleShape
    ) {
        Row {
            Column(Modifier.width(80.dp).background(MaterialTheme.colorScheme.surface)) {
                Row(Modifier
                    .background(track.color)
                    .clickable { EchoInMirror.selectedTrack = track }
                    .padding(vertical = 2.5.dp)
                ) {
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
                            DefaultTrack(
                                modifier,
                                progress,
                                interactionSource,
                                tickFractions,
                                enabled,
                                isVertical,
                                startPoint = 0.5f
                            )
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
                    TextButton({ }, Modifier.height(30.dp).fillMaxWidth()) {
                        Text(
                            "加载...",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            maxLines = 1,
                            lineHeight = 7.sp
                        )
                    }
                }
            }

            if (renderChildren && track.subTracks.isNotEmpty()) {
                Row {
                    Box(Modifier.width(2.dp).background(MaterialTheme.colorScheme.primary).padding(horizontal = 2.dp))
                    track.subTracks.forEachIndexed { i, it ->
                        key(it) { MixerTrack(it, i + 1) }
                    }
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
        BottomContentScrollable {
            Row(Modifier.padding(14.dp)) {
                MixerTrack(EchoInMirror.bus, 0, true, renderChildren = false)
                EchoInMirror.bus.subTracks.forEachIndexed { i, it ->
                    key(it) {
                        Gap(8)
                        MixerTrack(it, i + 1, true)
                    }
                }
            }
        }
    }
}
