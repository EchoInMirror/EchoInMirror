package cn.apisium.eim.impl

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.IS_DEBUG
import cn.apisium.eim.ROOT_PATH
import cn.apisium.eim.api.*
import cn.apisium.eim.api.processor.NativeAudioPluginDescription
import cn.apisium.eim.commands.*
import cn.apisium.eim.data.midi.getMidiEvents
import cn.apisium.eim.data.midi.getNoteMessages
import cn.apisium.eim.impl.processor.nativeAudioPluginManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
//import cn.apisium.eim.api.processor.NativeAudioPluginDescription
//import cn.apisium.eim.impl.processor.nativeAudioPluginManager
import kotlinx.coroutines.*
import java.io.File
import javax.sound.midi.MidiSystem
import kotlin.io.path.exists

@OptIn(ExperimentalComposeUiApi::class)
class CommandManagerImpl : CommandManager {
    override val commands = mutableMapOf<String, Command>()
    val commandMap = mutableMapOf<String, Command>()
    var customCommand = mutableMapOf<String, String>()
    private val customShortcutKeyPath = ROOT_PATH.resolve("shortcutKey.json")
    private val commandHandlers = mutableMapOf<Command, MutableSet<() -> Unit>>()

    init {
        registerCommand(DeleteCommand)
        registerCommand(CopyCommand)
        registerCommand(CutCommand)
        registerCommand(PasteCommand)
        registerCommand(SelectAllCommand)
        registerCommand(SaveCommand)

        registerCommand(OpenSettingsCommand)
        registerCommand(PlayOrPauseCommand)
        registerCommand(UndoCommand)
        registerCommand(RedoCommand)

        registerCommand(object : AbstractCommand("EIM:Temp", "Temp", arrayOf(Key.CtrlLeft, Key.R)) {
            override fun execute() {
                val apm = EchoInMirror.audioProcessorManager
                runBlocking {
                    val track = apm.createTrack()
                    val subTrack1 = apm.createTrack()
                    val subTrack2 = apm.createTrack()
                    track.name = "Track 1"
                    subTrack1.name = "SubTrack 1"
                    subTrack2.name = "SubTrack 2"
                    val factory = apm.audioProcessorFactories["EIMAudioProcessorFactory"]!!
                    val desc = factory.descriptions.find { it.name == "KarplusStrongSynthesizer" }!!
                    subTrack1.preProcessorsChain.add(factory.createAudioProcessor(desc))
                    if (IS_DEBUG) {
                        val midi = withContext(Dispatchers.IO) {
                            MidiSystem.getSequence(File("E:\\Midis\\UTMR&C VOL 1-14 [MIDI FILES] for other DAWs FINAL by Hunter UT\\VOL 13\\13.Darren Porter - To Feel Again LD.mid"))
                        }
                        val clip = EchoInMirror.clipManager.defaultMidiClipFactory.createClip()
                        clip.notes.addAll(getNoteMessages(midi.getMidiEvents(1)))
                        track.clips.add(
                            EchoInMirror.clipManager.createTrackClip(
                                clip,
                                0,
                                4 * 32 * EchoInMirror.currentPosition.ppq
                            )
                        )
                        val time = EchoInMirror.currentPosition.oneBarPPQ
                        val clip2 = EchoInMirror.clipManager.defaultMidiClipFactory.createClip()
                        subTrack1.clips.add(EchoInMirror.clipManager.createTrackClip(clip2, time, time))
                        val audioClip = EchoInMirror.clipManager.defaultAudioClipFactory.createClip()
                        subTrack2.clips.add(EchoInMirror.clipManager.createTrackClip(audioClip))

                        var proQ: NativeAudioPluginDescription? = null
                        var spire: NativeAudioPluginDescription? = null
                        EchoInMirror.audioProcessorManager.nativeAudioPluginManager.descriptions.forEach {
                            if (it.name == "FabFilter Pro-Q 3") proQ = it
                            if (it.name == "Spire-1.5") spire = it
                        }
//                        subTrack2.preProcessorsChain.add(EchoInMirror.audioProcessorManager.nativeAudioPluginManager.createAudioProcessor(spire!!))
                        track.postProcessorsChain.add(EchoInMirror.audioProcessorManager.nativeAudioPluginManager.createAudioProcessor(proQ!!))
                    }

                    track.subTracks.add(subTrack1)
                    track.subTracks.add(subTrack2)

                    EchoInMirror.bus?.subTracks?.add(track)
                    EchoInMirror.selectedTrack = track
                }
            }
        })

        if (customShortcutKeyPath.exists()) {
            customCommand = ObjectMapper().readValue(customShortcutKeyPath.toFile())
        }
    }

    fun saveCustomShortcutKeys() {
        ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(customShortcutKeyPath.toFile(), customCommand)
    }

    override fun registerCommand(command: Command) {
        commands[command.keyBindings.getKeys()] = command
        commandMap[command.name] = command
        commandHandlers[command] = hashSetOf()
    }

    override fun registerCommandHandler(command: Command, handler: () -> Unit) {
        commandHandlers[command]?.add(handler)
            ?: throw IllegalArgumentException("Command ${command.name} not registered")
    }

    override fun executeCommand(command: String) {
        val cmd = commandMap[customCommand[command]] ?: commands[command] ?: return
        try {
            cmd.execute()
            commandHandlers[cmd]!!.forEach { it() }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
