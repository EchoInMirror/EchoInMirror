package cn.apisium.eim

import cn.apisium.eim.components.app.eimApp
import cn.apisium.eim.data.midi.getMidiEvents
import cn.apisium.eim.data.midi.getNoteMessages
import cn.apisium.eim.impl.TrackImpl
import cn.apisium.eim.impl.processor.NativeAudioPluginImpl
import cn.apisium.eim.impl.processor.nativeAudioPluginManager
import cn.apisium.eim.processor.synthesizer.KarplusStrongSynthesizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.sound.midi.MidiSystem
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    createDirectories()
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.bus::close))
    Runtime.getRuntime().addShutdownHook(Thread(EchoInMirror.player::close))

    val track = TrackImpl("Track 1")
    val subTrack1 = TrackImpl("SubTrack 1")
    val subTrack2 = TrackImpl("SubTrack 2")
    subTrack1.preProcessorsChain.add(KarplusStrongSynthesizer())
    EchoInMirror.selectedTrack = track
    if (IS_DEBUG) {
        val midi = MidiSystem.getSequence(File("E:\\Midis\\UTMR&C VOL 1-14 [MIDI FILES] for other DAWs FINAL by Hunter UT\\VOL 13\\13.Darren Porter - To Feel Again LD.mid"))
        track.notes.addAll(getNoteMessages(midi.getMidiEvents(1)))
    }

    track.subTracks.add(subTrack1)
    track.subTracks.add(subTrack2)
    EchoInMirror.bus.subTracks.add(track)
    EchoInMirror.player.open(EchoInMirror.currentPosition.sampleRate, EchoInMirror.currentPosition.bufferSize, 2)

    if (IS_DEBUG) Thread {
        runBlocking {
            launch {
                delay(2000)
                var proQ: NativeAudioPluginImpl? = null
                var spire: NativeAudioPluginImpl? = null
                EchoInMirror.audioProcessorManager.nativeAudioPluginManager.descriptions.forEach {
                    if (it.name == "FabFilter Pro-Q 3") proQ = NativeAudioPluginImpl(it)
                    if (it.name == "Spire-1.5") spire = NativeAudioPluginImpl(it)
                }
                proQ!!.launch()
                spire!!.launch()
                subTrack2.preProcessorsChain.add(spire!!)
                track.postProcessorsChain.add(proQ!!)
            }
        }
    }.start()

    eimApp()
}
