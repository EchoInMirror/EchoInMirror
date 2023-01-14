package com.eimsound.daw.impl.clips

import com.eimsound.daw.api.Clip
import com.eimsound.daw.api.ClipFactory
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.impl.clips.audio.AudioClipFactoryImpl
import com.eimsound.daw.impl.clips.midi.MidiClipFactoryImpl
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class ClipManagerImpl : ClipManager {
    override val factories = HashMap<String, ClipFactory<*>>()

    init {
        registerClipFactory(MidiClipFactoryImpl())
        registerClipFactory(AudioClipFactoryImpl())
    }

    override fun registerClipFactory(factory: ClipFactory<*>) {
        factories[factory.name] = factory
    }

    override suspend fun createClip(factory: String) =
        factories[factory]?.createClip() ?: throw NoSuchFactoryException(factory)

    override suspend fun createClip(path: String, json: JsonNode) =
        factories[json["factory"].asText()]?.createClip(path, json) ?: throw NoSuchFactoryException(json["factory"].asText())

    override suspend fun createClip(path: String, id: String) =
        createClip(path, ObjectMapper().readTree(File(path, "$id.json")))

    override fun <T : Clip> createTrackClip(clip: T, time: Int, duration: Int, start: Int, track: Track?) =
        TrackClipImpl(clip, time, duration, start, track)
    override suspend fun createTrackClip(path: String, json: JsonNode) =
        TrackClipImpl(createClip(path, json["clip"]!!.asText()), json["time"]!!.asInt(), json["duration"]!!.asInt())
}