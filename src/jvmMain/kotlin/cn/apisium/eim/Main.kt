package cn.apisium.eim

import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.components.app.eimApp
import cn.apisium.eim.impl.TrackImpl
import cn.apisium.eim.impl.processor.NativeAudioPluginImpl
import cn.apisium.eim.processor.SineWaveSynthesizer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.bus::close))
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.player::close))

    val track = TrackImpl("Track 1")
    track.addProcessor(SineWaveSynthesizer(440.0))
    val plugin = NativeAudioPluginImpl(Json.decodeFromString(NativeAudioPluginDescription.serializer(), Files.readString(
        Paths.get("plugin.json"))))
    runBlocking {
        launch { plugin.launch() }
    }
    track.addProcessor(plugin)

    val subTrack = TrackImpl("SubTrack")
    track.addSubTrack(subTrack)
    EchoInMirror.bus.addSubTrack(track)

    EchoInMirror.player.open(EchoInMirror.sampleRate, EchoInMirror.bufferSize, 2)

    eimApp()
}
