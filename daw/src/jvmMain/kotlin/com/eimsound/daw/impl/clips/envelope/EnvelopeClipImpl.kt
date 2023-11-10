package com.eimsound.daw.impl.clips.envelope

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.*
import com.eimsound.audioprocessor.data.*
import com.eimsound.audioprocessor.data.midi.MidiNoteRecorder
import com.eimsound.daw.api.*
import com.eimsound.daw.api.controllers.DefaultParameterControllerFactory
import com.eimsound.daw.api.controllers.ParameterController
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.json.putNotDefault
import com.eimsound.daw.components.EnvelopeEditor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.nio.file.Path

class EnvelopeClipImpl(factory: ClipFactory<EnvelopeClip>): AbstractClip<EnvelopeClip>(factory), EnvelopeClip {
    override val envelope = DefaultEnvelopePointList()
    override val controllers = mutableStateListOf<ParameterController>()

    override fun toJson() = buildJsonObject {
        put("id", id)
        put("factory", factory.name)
        putNotDefault("controllers", controllers.map { it.toJson() })
        putNotDefault("envelope", envelope)
    }

    override fun fromJson(json: JsonElement) {
        super.fromJson(json)
        json as JsonObject
        controllers.clear()
        json["controllers"]?.let {
            EchoInMirror.bus?.onLoaded {
                it.jsonArray.fastForEach { e ->
                    controllers.add(DefaultParameterControllerFactory.createController(e.jsonObject))
                }
            }
        }
        json["envelope"]?.let { envelope.fromJson(it) }
    }
}

private val logger = KotlinLogging.logger { }
class EnvelopeClipFactoryImpl: EnvelopeClipFactory {
    override val name = "AudioClip"

    override fun createClip() = EnvelopeClipImpl(this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(path: Path, json: JsonObject) = EnvelopeClipImpl(this).apply {
        logger.info { "Creating clip ${json["id"]} in $path" }
        fromJson(json)
    }
    override fun getEditor(clip: TrackClip<EnvelopeClip>) = EnvelopeClipEditor()

    override fun processBlock(
        clip: TrackClip<EnvelopeClip>, buffers: Array<FloatArray>, position: CurrentPosition,
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteRecorder, pendingNoteOns: LongArray
    ) {
    }

    @Composable
    override fun PlaylistContent(
        clip: TrackClip<EnvelopeClip>, track: Track, contentColor: Color,
        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    ) {
        remember(clip) {
            EnvelopeEditor(clip.clip.envelope, VOLUME_RANGE, 1F, true)
        }.Editor(startPPQ, contentColor, noteWidth, false, clipStartTime = clip.start, stroke = 0.5F)
    }

    override fun save(clip: EnvelopeClip, path: Path) { }

    override fun toString(): String {
        return "MidiClipFactoryImpl(name='$name')"
    }
}