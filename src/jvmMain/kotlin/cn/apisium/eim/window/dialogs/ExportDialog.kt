package cn.apisium.eim.window.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.impl.RendererImpl
import java.awt.Dimension
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.zIndex
import cn.apisium.eim.api.convertPPQToSamples
import cn.apisium.eim.components.MenuItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioFileFormat

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[ExportDialog] = false
}

data class ExportFormate(val extend: String, val isLossLess: Boolean = true, val formate: AudioFileFormat.Type? = null)

@Composable
private fun Menu(expanded: Boolean = false, menuItems: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column {
        content()
        if (expanded) {
            Layout({
                Surface(
                    tonalElevation = 5.dp, shadowElevation = 5.dp
                ) {
                    val stateVertical = rememberScrollState(0)
                    Box(Modifier.verticalScroll(stateVertical).graphicsLayer(alpha = 1f, shadowElevation = 2F)) {
                        menuItems()
                    }
                }
            }) { measurable, constraints ->
                val placeable = measurable.firstOrNull()?.measure(constraints)
                layout(constraints.minWidth, constraints.minHeight) {
                    placeable!!.place(0, 0)
                }
            }
        }
    }
}

val ExportDialog = @Composable {
    Dialog(::closeQuickLoadWindow, title = "导出") {
        window.minimumSize = Dimension(300, 300)
        window.isModal = false
        Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {

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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("导出格式 ")
                            var expanded by remember { mutableStateOf(false) }
                            Menu(expanded, {
                                Column {
                                    exportOptions.map {
                                        MenuItem(
                                            exportSelect == it, {
                                                expanded = false
                                                exportSelect = it
                                            }
                                        ) {
                                            Text(it.extend)
                                        }
                                    }
                                }
                            }) {
                                Row(Modifier.clickable {
                                    expanded = !expanded
                                }) {
                                    Text(exportSelect.extend)
                                    Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }

                        Spacer(Modifier.width(40.dp))

                        val soundOptions = listOf<String>("立体声", "左声道", "右声道")
                        var soundSelect by remember { mutableStateOf(soundOptions[0]) }
                        Row {
                            var expanded by remember { mutableStateOf(false) }
                            Menu(expanded, {
                                Column {
                                    soundOptions.map {
                                        MenuItem(
                                            soundSelect == it, {
                                                expanded = false
                                                soundSelect = it
                                            }
                                        ) {
                                            Text(it)
                                        }
                                    }
                                }
                            }) {
                                Row(Modifier.clickable {
                                    expanded = !expanded
                                }) {
                                    Text(soundSelect)
                                    Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                    Row {
                        if (exportSelect.isLossLess) {
                            val deepList = listOf<Int>(32, 24, 16)
                            var deep by remember { mutableStateOf(deepList[0]) }
                            Row {
                                Text("位深 ")
                                var expanded by remember { mutableStateOf(false) }
                                Menu(expanded, {
                                    Column {
                                        deepList.map {
                                            MenuItem(
                                                deep == it, {
                                                    expanded = false
                                                    deep = it
                                                }
                                            ) {
                                                Text(it.toString())
                                            }
                                        }
                                    }
                                }) {
                                    Row(Modifier.clickable {
                                        expanded = !expanded
                                    }) {
                                        Text("${deep}位")
                                        Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                            }
                        }

                        if (!exportSelect.isLossLess) {
                            val bitRateOptions = listOf<Int>(128, 192, 256, 320)
                            var bitRate by remember { mutableStateOf(bitRateOptions[0]) }
                            Row {
                                Text("比特率 ")
                                var expanded by remember { mutableStateOf(false) }
                                Menu(expanded, {
                                    Column {
                                        bitRateOptions.map {
                                            MenuItem(
                                                bitRate == it, {
                                                    expanded = false
                                                    bitRate = it
                                                }
                                            ) {
                                                Text(it.toString())
                                            }
                                        }
                                    }
                                }) {
                                    Row(Modifier.clickable {
                                        expanded = !expanded
                                    }) {
                                        Text("${bitRate}kbps")
                                        Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                            }
                        }

                        if (exportSelect.extend == "flac") {
                            Spacer(Modifier.width(10.dp))
                            val flacCompressOptions = 0..8
                            var flacCompress by remember { mutableStateOf(5) }
                            Row {
                                Text("flac压缩 ")
                                var expanded by remember { mutableStateOf(false) }
                                Menu(expanded, {
                                    Column {
                                        flacCompressOptions.map {
                                            MenuItem(
                                                flacCompress == it, {
                                                    expanded = false
                                                    flacCompress = it
                                                }
                                            ) {
                                                Text(it.toString())
                                            }
                                        }
                                    }
                                }) {
                                    Row(Modifier.clickable {
                                        expanded = !expanded
                                    }) {
                                        Text(flacCompress.toString())
                                        Icon(Icons.Filled.ExpandMore, null, Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(100.dp))
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
