package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.zIndex
import com.eimsound.audioprocessor.RenderFormat
import com.eimsound.audioprocessor.convertPPQToSamples
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.ChannelType
import com.eimsound.daw.components.*
import com.eimsound.daw.utils.range
import com.eimsound.dsp.native.JvmRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[ExportDialog] = false
}

private fun formatSecondTime(timeInSecond: Float): String {
    return "${(timeInSecond / 60).toInt().toString().padStart(2, '0')}:${
        (timeInSecond % 60).toInt().toString().padStart(2, '0')
    }"//:${((timeInSecond * 1000) % 1000).toInt().toString().padStart(3, '0')}
}

private val floatingLayerProvider = FloatingLayerProvider()

@OptIn(DelicateCoroutinesApi::class)
val ExportDialog = @Composable {
    DialogWindow(::closeQuickLoadWindow, title = "导出", content = fun DialogWindowScope.() {
        remember { EchoInMirror.currentPosition.isPlaying = false }
        window.minimumSize = Dimension(300, 500)

        CompositionLocalProvider(LocalFloatingLayerProvider.provides(floatingLayerProvider)) {
            Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
                Box(Modifier.padding(10.dp)) {
                    Column {
                        var isRendering by remember { mutableStateOf(false) }

                        val position = EchoInMirror.currentPosition
                        val endPPQ = position.projectRange.range
                        val timeInSecond = position.convertPPQToSamples(endPPQ).toFloat() / position.sampleRate
                        var filename by remember {
                            mutableStateOf(
                                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)
                            )
                        }
                        var renderFormat by remember { mutableStateOf(RenderFormat.WAV) }
                        var soundSelect by remember { mutableStateOf(ChannelType.STEREO) }
                        var bits by remember { mutableStateOf(16) }
                        var bitRate by remember { mutableStateOf(320) }
                        var compressionLevel by remember { mutableStateOf(5) }


                        if (!isRendering) {
                            Row {
                                Text("长度: ")
                                Text(
                                    "${endPPQ / position.ppq / position.timeSigNumerator + 1}节",
                                    color = Color.Black.copy(0.5f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "总时间: "
                                )
                                Text(
                                    "${
                                        (timeInSecond / 60).toInt().toString().padStart(2, '0')
                                    }'${(timeInSecond % 60).toInt().toString().padStart(2, '0')}\"",
                                    color = Color.Black.copy(0.5f)
                                )
                                Spacer(Modifier.width(10.dp))
                            }

                            Spacer(Modifier.height(10.dp))
                            Divider()
                            Spacer(Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("文件名:    ")
                                TextField(filename, { filename = it }, modifier = Modifier.width(200.dp))
                            }
                            Spacer(Modifier.height(5.dp))

                            Row {
                                Row(
                                    Modifier.weight(1F),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("导出格式: ")
                                    Menu({ close ->
                                        RenderFormat.values().forEach {
                                            MenuItem(
                                                {
                                                    close()
                                                    renderFormat = it
                                                },
                                                renderFormat == it,
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text(it.extend) }
                                        }
                                    }) {
                                        Text(
                                            renderFormat.extend
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))


                                Row(Modifier.weight(1F)) {
                                    Menu({ close ->
                                        Column {
                                            ChannelType.values().forEach {
                                                MenuItem({
                                                    close()
                                                    soundSelect = it
                                                }, soundSelect == it, modifier = Modifier.fillMaxWidth()) {
                                                    Text(it.name)
                                                }
                                            }
                                        }
                                    }) {
                                        Text(soundSelect.name)
                                    }
                                }
                            }

                            Spacer(Modifier.height(5.dp))


                            if (renderFormat.isLossLess) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("位深: ", Modifier.width(80.dp))
                                    arrayOf(16, 24, 32).map {
                                        RadioButton(bits == it, { bits = it })
                                        Text("${it}位")

                                    }
                                }
                            }


                            if (!renderFormat.isLossLess) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("比特率: ", Modifier.width(80.dp))
                                    Menu({ close ->
                                        Column {
                                            arrayOf(128, 192, 256, 320).map {
                                                MenuItem({
                                                    close()
                                                    bitRate = it
                                                }, bitRate == it, modifier = Modifier.fillMaxWidth()) {
                                                    Text(it.toString())
                                                }
                                            }
                                        }
                                    }) {
                                        Text("${bitRate}kbps")
                                    }
                                }
                            }


                            if (renderFormat.extend == "flac") {
                                Spacer(Modifier.width(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("flac压缩: ", Modifier.width(80.dp))
                                    Slider(
                                        compressionLevel.toFloat(),
                                        { compressionLevel = it.toInt() },
                                        valueRange = 0f..8f,
                                        steps = 7,
                                        modifier = Modifier.weight(5f)
                                    )
                                    Text(" $compressionLevel", modifier = Modifier.weight(1f))
                                }
                            }
                            Filled()
                        }


                        var renderProcess by remember { mutableStateOf(0f) }
                        var renderJob by remember { mutableStateOf<Job?>(null) }
                        var renderStartTime by remember { mutableStateOf(0L) }
                        var lastRenderBlockTime: Long
                        var renderRate by remember { mutableStateOf(1f) }
                        var renderBlockSize: Int
                        if (isRendering) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("已用时: ${formatSecondTime((System.currentTimeMillis() - renderStartTime) / 1000F)}")
                                Text("剩余: ${formatSecondTime(timeInSecond * (1 - renderProcess) / renderRate)}")
                                Text("已渲染: ${formatSecondTime(timeInSecond * renderProcess)}")
                                Text("%.1f 倍快于实时".format(renderRate))
                                Text("${(renderProcess * 100).toInt()}%")
                                Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                    LinearProgressIndicator(renderProcess, Modifier.fillMaxWidth())
                                }
                            }
                        }
                        Button({
                            if (isRendering) {
                                if (!renderJob!!.isCompleted)
                                    renderJob?.cancel()
                                isRendering = false
                            } else {
                                if (filename.isEmpty()) return@Button
                                val renderer = EchoInMirror.bus?.let { JvmRenderer(it) }
                                val curPosition = EchoInMirror.currentPosition
                                val audioFile = File("./${filename}.${renderFormat.extend}")

                                isRendering = true
                                renderStartTime = System.currentTimeMillis()
                                renderBlockSize = 0
                                lastRenderBlockTime = 0

                                window.size = Dimension(300, 200)

                                EchoInMirror.player?.close()
                                renderJob = GlobalScope.launch {
                                    renderer?.start(
                                        curPosition.projectRange,
                                        curPosition.sampleRate,
                                        curPosition.ppq,
                                        curPosition.bpm,
                                        audioFile,
                                        renderFormat,
                                        bits,
                                        bitRate,
                                        compressionLevel
                                    ) {
                                        renderProcess = it
                                        if (it < 1f) {
                                            renderBlockSize += curPosition.bufferSize
                                            if (renderBlockSize >= curPosition.sampleRate) {
                                                val newTime = System.currentTimeMillis()
                                                renderRate = 1000f / (newTime - lastRenderBlockTime)
                                                lastRenderBlockTime = newTime
                                                renderBlockSize = 0
                                            }
                                        } else {
                                            isRendering = false
                                            EchoInMirror.player = EchoInMirror.createAudioPlayer()
                                        }
                                    }
                                }
                            }
                        }, Modifier.zIndex(-10f).fillMaxWidth()) {
                            Row {
                                if (isRendering && renderProcess < 1f) Text("取消")
                                else if (isRendering && renderProcess >= 1f) Text("确认")
                                else Text("导出到 $filename.${renderFormat.extend}")
                            }
                        }
                    }
                }
            }
        }
        floatingLayerProvider.FloatingLayers()
    })
}
