package cn.apisium.eim.window

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.Gap
import cn.apisium.eim.components.Level
import cn.apisium.eim.components.Marquee
import cn.apisium.eim.components.silder.DefaultTrack
import cn.apisium.eim.components.silder.Slider
import cn.apisium.eim.toOnSurface

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
        Row(Modifier.padding(14.dp)) {
            Surface(Modifier.width(80.dp), shadowElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                val trackColor = MaterialTheme.colorScheme.primary
                Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.background(trackColor).fillMaxWidth().padding(vertical = 2.5.dp)) {
                        Text("1",
                            Modifier.padding(start = 5.dp, end = 3.dp),
                            color = trackColor.toOnSurface,
                            fontSize = MaterialTheme.typography.labelLarge.fontSize,
                            lineHeight = 18.0.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Marquee {
                            Text("Track 11111111111",
                                color = trackColor.toOnSurface,
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                lineHeight = 18.0.sp,
                            )
                        }
                    }

                    Column(Modifier.padding(4.dp)) {
                        var stateSlider2 by remember { mutableStateOf(0f) }
                        var stateSlider by remember { mutableStateOf(0f) }
                        Slider(
                            stateSlider2,
                            { stateSlider2 = it },
                            valueRange = -1f..1f,
                            modifier = Modifier.fillMaxWidth(),
                            thumbSize = DpSize(14.dp, 14.dp),
                            track = { modifier, progress, interactionSource, tickFractions, enabled, isVertical ->
                                DefaultTrack(modifier, progress, interactionSource, tickFractions, enabled, isVertical, startPoint = 0.5f)
                            }
                        )

                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ }, Modifier.size(20.dp)) {
                                Icon(Icons.Default.VolumeUp, "Volume")
                            }
                            Gap(8)
                            Text("-12.0",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                letterSpacing = (-1).sp,
                                lineHeight = 12.sp
                            )
                        }

                        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                stateSlider,
                                { stateSlider = it },
                                valueRange = 0f..140f,
                                modifier = Modifier.height(150.dp),
                                thumbSize = DpSize(14.dp, 14.dp),
                                isVertical = true,
                                track = { modifier, progress, interactionSource, tickFractions, enabled, isVertical ->
                                    DefaultTrack(modifier, progress, interactionSource, tickFractions, enabled, isVertical, startPoint = 1f, stroke = 3.dp)
                                }
                            )
                            Gap(18)
                            Level(0f, 0f, Modifier.height(136.dp))
                        }
                    }

                    Column {
                        Divider(Modifier.padding(horizontal = 8.dp))
                        TextButton({ }, Modifier.height(30.dp).fillMaxWidth()) {
                            Text("加载插件",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                maxLines = 1,
                                lineHeight = 7.sp)
                        }
                    }
                }
            }
        }
    }
}
