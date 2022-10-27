package cn.apisium.eim

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cn.apisium.eim.api.AudioPlayer
import cn.apisium.eim.api.CurrentPosition
import cn.apisium.eim.api.processor.PluginDescription
import cn.apisium.eim.components.eimAppBar
import cn.apisium.eim.components.sideBar
import cn.apisium.eim.components.statusBar
import cn.apisium.eim.impl.AudioPluginImpl
import cn.apisium.eim.impl.BusImpl
import cn.apisium.eim.impl.TrackImpl
import cn.apisium.eim.impl.players.JvmAudioPlayer
import cn.apisium.eim.processor.SineWaveSynthesizer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

val currentPosition = CurrentPosition()
val bus = BusImpl()
private var player_: AudioPlayer = JvmAudioPlayer(currentPosition, bus)
var sampleRate by mutableStateOf(44800F)
var bufferSize by mutableStateOf(1024)
var timeSigNumerator by mutableStateOf(4)
var timeSigDenominator by mutableStateOf(4)
var player: AudioPlayer
    get() = player_
    set(value) { player_ = value }

@Composable
fun checkSampleRateAndBufferSize(): Array<Any> {
    SideEffect {
        println("Changed: $sampleRate $bufferSize")
    }
    return arrayOf(sampleRate, bufferSize)
}

@OptIn(ExperimentalMaterial3Api::class)
fun main() {
    Runtime.getRuntime().addShutdownHook(Thread(bus::close))

    val track = TrackImpl("Track 1")
    track.addProcessor(SineWaveSynthesizer(440.0))
    val plugin = AudioPluginImpl(Json.decodeFromString(PluginDescription.serializer(), Files.readString(Paths.get("plugin.json"))))
    runBlocking {
        launch { plugin.launch() }
    }
    track.addProcessor(plugin)

    bus.addTrack(track)

    player.open(sampleRate, bufferSize, 2)

    application {
        val icon = painterResource("logo.png")
        checkSampleRateAndBufferSize()
        Window(onCloseRequest = ::exitApplication, icon = icon, title = "Echo In Mirror") {
            MaterialTheme {
                Row {
                    sideBar()
                    Scaffold(
                        topBar = { eimAppBar() },
                        content = {
                            Box(Modifier.fillMaxSize()) {

                            }
                        },
                        bottomBar = { statusBar() }
                    )
                }
            }
        }
    }
}
