package cn.apisium.eim

import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.components.app.eimApp
import cn.apisium.eim.data.midi.getMidiEvents
import cn.apisium.eim.data.midi.getNoteMessages
import cn.apisium.eim.impl.TrackImpl
import cn.apisium.eim.impl.processor.NativeAudioPluginImpl
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.sound.midi.MidiSystem
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.bus::close))
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.player::close))

    val track = TrackImpl("Track 1")
    track.preProcessorsChain.add(KarplusStrongSynthesizer())
    val plugin = NativeAudioPluginImpl(Json.decodeFromString(NativeAudioPluginDescription.serializer(), Files.readString(
        Paths.get("plugin.json"))))
    runBlocking {
        launch { plugin.launch() }
    }
    track.postProcessorsChain.add(plugin)
    val midi = MidiSystem.getSequence(File("E:\\Midis\\UTMR&C VOL 1-14 [MIDI FILES] for other DAWs FINAL by Hunter UT\\VOL 13\\13.Darren Porter - To Feel Again LD.mid"))
    track.notes.addAll(getNoteMessages(midi.getMidiEvents(1)))

    val subTrack = TrackImpl("SubTrack")
    track.subTracks.add(subTrack)
    EchoInMirror.bus.subTracks.add(track)

    EchoInMirror.player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)

    EchoInMirror.selectedTrack = track

    eimApp()
}
