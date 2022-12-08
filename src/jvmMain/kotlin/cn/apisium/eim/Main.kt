package cn.apisium.eim

import cn.apisium.eim.components.app.eimApp
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.bus::close))

//    val track = TrackImpl("Track 1")
//    track.addProcessor(SineWaveSynthesizer(440.0))
//    val plugin = NativeAudioPluginImpl(Json.decodeFromString(NativeAudioPluginDescription.serializer(), Files.readString(Paths.get("plugin.json"))))
//    runBlocking {
//        launch { plugin.launch() }
//    }
//    track.addProcessor(plugin)
//
//    EchoInMirror.bus.addTrack(track)

    EchoInMirror.player.open(EchoInMirror.sampleRate, EchoInMirror.bufferSize, 2)

    eimApp()
}
