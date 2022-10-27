package cn.apisium.eim

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

@OptIn(ExperimentalMaterial3Api::class)
fun main() {
    val bus = BusImpl()
    Runtime.getRuntime().addShutdownHook(Thread(bus::close))

    val track = TrackImpl("Track 1")
    track.addProcessor(SineWaveSynthesizer(440.0))
    val plugin = AudioPluginImpl(Json.decodeFromString(PluginDescription.serializer(), Files.readString(Paths.get("plugin.json"))))
    runBlocking {
        launch { plugin.launch() }
    }
    track.addProcessor(plugin)

    bus.addTrack(track)

    val player = JvmAudioPlayer()
    player.processor = bus
    player.open(44800F, 1024, 2)

    application {
        val icon = painterResource("logo.png")
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
