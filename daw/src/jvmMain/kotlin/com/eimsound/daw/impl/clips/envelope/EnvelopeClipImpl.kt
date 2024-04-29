package com.eimsound.daw.impl.clips.envelope

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach
import com.eimsound.audioprocessor.*
import com.eimsound.daw.actions.GlobalEnvelopeEditorEventHandler
import com.eimsound.daw.api.*
import com.eimsound.daw.api.clips.*
import com.eimsound.daw.api.controllers.DefaultParameterControllerFactory
import com.eimsound.daw.api.controllers.ParameterController
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.commons.json.putNotDefault
import com.eimsound.daw.components.EnvelopeEditor
import com.eimsound.daw.language.langs
import com.eimsound.dsp.data.*
import com.eimsound.dsp.data.midi.MidiNoteTimeRecorder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.nio.file.Path

class EnvelopeClipImpl(factory: ClipFactory<EnvelopeClip>): AbstractClip<EnvelopeClip>(factory), EnvelopeClip {
    override val name
        get() = "${langs.envelope} (${if (controllers.size > 4) "${controllers.size}${langs.controller}"
            else controllers.joinToString(", ") { it.parameter.name }})"
    override val envelope = DefaultEnvelopePointList()
    override val controllers = mutableStateListOf<ParameterController>()
    override val isExpandable = true
    override val icon = Icons.Default.Timeline

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

    override fun copy() = EnvelopeClipImpl(factory).also {
        it.envelope.addAll(envelope.copy())
        it.controllers.addAll(controllers)
    }
}

private val logger = KotlinLogging.logger { }
class EnvelopeClipFactoryImpl: EnvelopeClipFactory {
    override val name = "EnvelopeClip"

    override fun createClip() = EnvelopeClipImpl(this).apply {
        logger.info { "Creating clip \"${this.id}\"" }
    }
    override fun createClip(path: Path, json: JsonObject) = EnvelopeClipImpl(this).apply {
        logger.info { "Creating clip ${json["id"]} in $path" }
        fromJson(json)
    }
    override fun getEditor(clip: TrackClip<EnvelopeClip>) = EnvelopeClipEditor(clip)

    override fun processBlock(
        clip: TrackClip<EnvelopeClip>, buffers: Array<FloatArray>, position: PlayPosition,
        midiBuffer: ArrayList<Int>, noteRecorder: MidiNoteTimeRecorder
    ) {
        val clipTime = clip.time - clip.start
        val value = clip.clip.envelope.getValue(position.timeInPPQ - clipTime, 1F)
        clip.clip.controllers.fastForEach { it.parameter.value = value }
    }

    @Composable
    override fun PlaylistContent(
        clip: TrackClip<EnvelopeClip>, track: Track, contentColor: Color,
        noteWidth: MutableState<Dp>, startPPQ: Float, widthPPQ: Float
    ) {
        val ctrl = clip.clip.controllers.firstOrNull()?.parameter
        remember(clip, ctrl) {
            if (ctrl == null) return@remember null
            EnvelopeEditor(
                clip.clip.envelope, ctrl.range, ctrl.initialValue, ctrl.isFloat,
                null, GlobalEnvelopeEditorEventHandler
            )
        }?.Editor(
            startPPQ, contentColor, noteWidth, true, clipStartTime = clip.start, stroke = 1F,
            backgroundColor = track.color.copy(0.7F).compositeOver(MaterialTheme.colorScheme.background)
        )
    }

    override fun split(clip: TrackClip<EnvelopeClip>, time: Int): ClipSplitResult<EnvelopeClip> {
        val newClip = EnvelopeClipImpl(this)
        newClip.controllers.addAll(clip.clip.controllers)
        val oldEnvelopes = clip.clip.envelope.toList()
        val (left, right) = clip.clip.envelope.split(time, 0)
        clip.clip.envelope.clear()
        clip.clip.envelope.addAll(left)
        newClip.envelope.addAll(right)

        return object : ClipSplitResult<EnvelopeClip> {
            override val clip = newClip
            override val start = 0
            override fun revert() {
                clip.clip.envelope.clear()
                clip.clip.envelope.addAll(oldEnvelopes)
                clip.clip.envelope.update()
            }
        }
    }

    override fun save(clip: EnvelopeClip, path: Path) { }

    override fun toString() = "EnvelopeClipFactoryImpl"

    override fun canMerge(clip: TrackClip<*>) = clip.clip is EnvelopeClip
    override fun merge(clips: Collection<TrackClip<*>>): List<ClipActionResult<EnvelopeClip>> {
        val newClip = EnvelopeClipImpl(this)
        var start = Int.MAX_VALUE
        var end = Int.MIN_VALUE
        val newPoints = hashMapOf<Int, EnvelopePoint>()
        val controllers = mutableSetOf<ParameterController>()
        clips.forEach {
            if (it.clip !is EnvelopeClip) return@forEach
            start = start.coerceAtMost(it.time)
            end = end.coerceAtLeast(it.time + it.duration)
        }
        if (start == Int.MAX_VALUE || end == Int.MIN_VALUE) return emptyList()
        clips.forEach { c ->
            val clip = c.clip as? EnvelopeClip ?: return@forEach
            controllers.addAll(clip.controllers)
            clip.envelope.fastForEach {
                if (it.time + c.time >= c.start && it.time <= c.duration + c.start) {
                    val time = it.time + c.time - start - c.start
                    newPoints[time] = it.copy(time = time)
                }
            }
        }
        newClip.envelope.addAll(newPoints.values)
        newClip.envelope.sort()
        newClip.controllers.addAll(controllers)
        return listOf(ClipActionResult(newClip, start, end - start))
    }
}