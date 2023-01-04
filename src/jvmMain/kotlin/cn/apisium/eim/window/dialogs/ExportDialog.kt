package cn.apisium.eim.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.impl.RendererImpl
import java.awt.Dimension
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import cn.apisium.eim.api.convertPPQToSamples
import cn.apisium.eim.components.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioFileFormat

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[ExportDialog] = false
}

data class ExportFormate(val extend: String, val isLossLess: Boolean = true, val formate: AudioFileFormat.Type? = null)

private val floatingDialogProvider = FloatingDialogProvider()

@Composable
private fun Menu(
    menuItems: @Composable (closeDialog: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    FloatingDialog({ _, close ->
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            shadowElevation = 6.dp, tonalElevation = 1.dp
        ) {
            val stateVertical = rememberScrollState(0)
            Column(Modifier.verticalScroll(stateVertical)) {
                menuItems(close)
            }
        }
    }, modifier = Modifier.zIndex(100f)) {
        Surface(
            shadowElevation = 2.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                Modifier.width(100.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                content()
                Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
            }
        }
    }

}

val ExportDialog = @Composable {
    Dialog(::closeQuickLoadWindow, title = "导出") {
        window.minimumSize = Dimension(300, 300)
        window.isModal = false
        CompositionLocalProvider(LocalFloatingDialogProvider.provides(floatingDialogProvider)) {
            Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
                floatingDialogProvider.FloatingDialogs()
                Box(Modifier.padding(10.dp)) {
                    Column {
                        Row {
                            val position = EchoInMirror.currentPosition
                            val endPPQ = position.projectRange.last - position.projectRange.first
                            Text("长度:")
                            Text(
                                "${endPPQ / position.ppq / position.timeSigNumerator + 1}节",
                                color = Color.Black.copy(0.5f)
                            )
                            Spacer(Modifier.width(10.dp))
                            val timeInSecond = position.convertPPQToSamples(endPPQ).toFloat() / position.sampleRate
                            Text(
                                "总时间:"
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
                        val exportOptions = listOf<ExportFormate>(
                            ExportFormate("wav", formate = AudioFileFormat.Type.WAVE),
                            ExportFormate("au", formate = AudioFileFormat.Type.AU),
                            ExportFormate("aif", formate = AudioFileFormat.Type.AIFF),
                            ExportFormate("aifc", formate = AudioFileFormat.Type.AIFC),
                            ExportFormate("snd", formate = AudioFileFormat.Type.SND),
                            ExportFormate("flac"),
                            ExportFormate("mp3", false),
                            ExportFormate("ogg", false)
                        )
                        var exportSelect by remember { mutableStateOf(exportOptions[0]) }
                        Row {
                            Row {
                                Text("导出格式 ", Modifier.width(80.dp))
                                Menu({ close ->
                                    exportOptions.map {
                                        MenuItem(
                                            exportSelect == it, {
                                                close()
                                                exportSelect = it
                                            }
                                        ) {
                                            Text(it.extend)
                                        }
                                    }
                                }) {
                                    Text(
                                        exportSelect.extend
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            val soundOptions = listOf<String>("立体声", "左声道", "右声道")
                            var soundSelect by remember { mutableStateOf(soundOptions[0]) }
                            Row {
                                Menu({ close ->
                                    Column {
                                        soundOptions.map {
                                            MenuItem(
                                                soundSelect == it, {
                                                    close()
                                                    soundSelect = it
                                                }
                                            ) {
                                                Text(it)
                                            }
                                        }
                                    }
                                }) {
                                    Text(soundSelect)
                                }
                            }
                        }

                        Spacer(Modifier.height(5.dp))

                        Row {
                            if (exportSelect.isLossLess) {
                                val deepList = listOf<Int>(32, 24, 16)
                                var deep by remember { mutableStateOf(deepList[0]) }
                                Row {
                                    Text("位深 ", Modifier.width(80.dp))
                                    Menu({ close ->
                                        Column {
                                            deepList.map {
                                                MenuItem(
                                                    deep == it, {
                                                        close()
                                                        deep = it
                                                    }
                                                ) {
                                                    Text(it.toString())
                                                }
                                            }
                                        }
                                    }) {
                                        Text("${deep}位")
                                    }
                                }
                            }

                            if (!exportSelect.isLossLess) {
                                val bitRateOptions = listOf<Int>(128, 192, 256, 320)
                                var bitRate by remember { mutableStateOf(bitRateOptions[0]) }
                                Row {
                                    Text("比特率 ", Modifier.width(80.dp))
                                    Menu({ close ->
                                        Column {
                                            bitRateOptions.map {
                                                MenuItem(
                                                    bitRate == it, {
                                                        close()
                                                        bitRate = it
                                                    }
                                                ) {
                                                    Text(it.toString())
                                                }
                                            }
                                        }
                                    }) {
                                        Text("${bitRate}kbps")
                                    }
                                }
                            }

                            if (exportSelect.extend == "flac") {
                                Spacer(Modifier.width(10.dp))
                                val flacCompressOptions = 0..8
                                var flacCompress by remember { mutableStateOf(5) }
                                Row {
                                    Text("flac压缩 ", Modifier.width(80.dp))
                                    Menu({ close ->
                                        Column {
                                            flacCompressOptions.map {
                                                MenuItem(
                                                    flacCompress == it, {
                                                        close()
                                                        flacCompress = it
                                                    }
                                                ) {
                                                    Text(it.toString())
                                                }
                                            }
                                        }
                                    }) {
                                        Text(flacCompress.toString())
                                    }
                                }
                            }
                        }
                        Filled()
                        Button(onClick = {
                            val renderer = EchoInMirror.bus?.let { RendererImpl(it) }
                            val position = EchoInMirror.currentPosition
                            val audioFile = File("./test.wav")
                            EchoInMirror.player!!.close()
                            GlobalScope.launch {
                                renderer?.start(
                                    position.projectRange,
                                    position.sampleRate,
                                    position.ppq,
                                    position.bpm,
                                    audioFile,
                                    AudioFileFormat.Type.WAVE
                                ) {
                                    println(it)
                                }
                            }
                        }, Modifier.zIndex(-10f).fillMaxWidth()) {
                            Row {
                                Text("导出到 test.${exportSelect.extend}")
                            }
                        }


                    }

                }
            }
        }

    }
}
