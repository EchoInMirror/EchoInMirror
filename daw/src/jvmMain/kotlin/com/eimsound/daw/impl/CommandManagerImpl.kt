package com.eimsound.daw.impl

import androidx.compose.ui.input.key.*
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.NativeAudioPluginDescription
import com.eimsound.audioprocessor.oneBarPPQ
import com.eimsound.daw.ROOT_PATH
import com.eimsound.daw.api.*
import com.eimsound.daw.api.clips.ClipManager
import com.eimsound.daw.api.clips.defaultMidiClipFactory
import com.eimsound.daw.api.processor.DefaultTrackAudioProcessorWrapper
import com.eimsound.daw.api.processor.TrackManager
import com.eimsound.daw.commands.*
import com.eimsound.daw.commons.json.toJson
import com.eimsound.daw.components.initEditorMenuItems
import com.eimsound.dsp.native.processors.nativeAudioPluginFactory
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlin.io.path.exists

class CommandManagerImpl : CommandManager {
    override val commands = mutableMapOf<String, Command>()
    val commandsMap = mutableMapOf<String, Command>()
    val customCommands = mutableMapOf<String, String>()
    private val customShortcutKeyPath = ROOT_PATH.resolve("shortcutKey.json")
    private val commandHandlers = mutableMapOf<Command, MutableSet<() -> Unit>>()

    init {
        registerAllEditCommands()
        registerAllUICommands()
        registerAllEditorToolCommands()

        registerCommand(object : AbstractCommand("EIM:Temp", "Temp", arrayOf(Key.CtrlLeft, Key.R)) {
            override fun execute() {
                val tm = TrackManager.instance
                runBlocking {
                    val track = tm.createTrack()
                    val subTrack1 = tm.createTrack()
                    val subTrack2 = tm.createTrack()
                    track.name = "Track 1"
                    subTrack1.name = "SubTrack 1"
                    subTrack2.name = "SubTrack 2"
                    val factory = AudioProcessorManager.instance.factories["EIMAudioProcessorFactory"]!!
                    val desc = factory.descriptions.find { it.name == "KarplusStrongSynthesizer" }!!
                    subTrack1.preProcessorsChain.add(DefaultTrackAudioProcessorWrapper(factory.createAudioProcessor(desc)))
                    val time = EchoInMirror.currentPosition.oneBarPPQ
//                    val clip1 = ClipManager.instance.defaultAudioClipFactory.createClip(Paths.get("C:\\Python311\\op.wav"))
//                    subTrack2.clips.add(ClipManager.instance.createTrackClip(clip1))
                    val clip2 = ClipManager.instance.defaultMidiClipFactory.createClip()
                    subTrack1.clips.add(ClipManager.instance.createTrackClip(clip2, time, time))
//                    val clip = ClipManager.instance.defaultMidiClipFactory.createClip()
//                    if (IS_DEBUG) {
//                        val midi = withContext(Dispatchers.IO) {
//                            MidiSystem.getSequence(File("E:\\Midis\\UTMR&C VOL 1-14 [MIDI FILES] for other DAWs FINAL by Hunter UT\\VOL 13\\13.Darren Porter - To Feel Again LD.mid"))
//                        }
//                        val clip = ClipManager.instance.defaultMidiClipFactory.createClip()
//                        clip.notes.addAll(getNoteMessages(midi.getMidiEvents(1, EchoInMirror.currentPosition.ppq)))
//                        track.clips.add(
//                            ClipManager.instance.createTrackClip(
//                                clip,
//                                0,
//                                4 * 32 * EchoInMirror.currentPosition.ppq
//                            )
//                        )
//                        val audioClip = EchoInMirror.clipManager.defaultAudioClipFactory.createClip()
//                        subTrack2.clips.add(EchoInMirror.clipManager.createTrackClip(audioClip))

//                        var proQ: NativeAudioPluginDescription? = null
                        var spire: NativeAudioPluginDescription? = null
                        val napm = AudioProcessorManager.instance.nativeAudioPluginFactory
                        napm.descriptions.forEach {
//                            if (it.name == "FabFilter Pro-Q 3") proQ = it
                            if (it.name == "Spire-1.5") spire = it
                        }
                        subTrack2.preProcessorsChain.add(DefaultTrackAudioProcessorWrapper(napm.createAudioProcessor(spire!!)))
//                        track.postProcessorsChain.add(napm.createAudioProcessor(proQ!!))
//                    }

                    track.subTracks.add(subTrack1)
                    track.subTracks.add(subTrack2)

                    EchoInMirror.bus?.subTracks?.add(track)
                    EchoInMirror.selectedTrack = track
                }
            }
        })

        if (customShortcutKeyPath.exists()) {
            customCommands.putAll(customShortcutKeyPath.toFile().toJson<Map<String, String>>())
        }

        initEditorMenuItems()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveCustomShortcutKeys() {
        Json.encodeToStream(customCommands, customShortcutKeyPath.toFile().outputStream())
    }

    override fun registerCommand(command: Command) {
        commands[command.keyBindings.getKeys()] = command
        commandsMap[command.name] = command
        commandHandlers[command] = hashSetOf()
    }

    override fun registerCommandHandler(command: Command, handler: () -> Unit) {
        commandHandlers[command]?.add(handler)
            ?: throw IllegalArgumentException("Command ${command.name} not registered")
    }

    override fun executeCommand(command: String) {
        val cmd = commandsMap[customCommands[command]] ?: commands[command] ?: return
        try {
            cmd.execute()
            commandHandlers[cmd]!!.forEach { it() }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun getKeysOfCommand(command: Command) =
        customCommands.firstNotNullOfOrNull { (key, value) ->
            if (value == command.name) key.split(" ").map { Key(it.toInt()) }.toTypedArray()
            else null
        } ?: command.keyBindings
}
