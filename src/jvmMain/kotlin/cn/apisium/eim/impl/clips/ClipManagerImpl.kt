package cn.apisium.eim.impl.clips

import cn.apisium.eim.api.*
import cn.apisium.eim.impl.clips.midi.MidiClipFactoryImpl
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class ClipManagerImpl : ClipManager {
    override val factories = HashMap<String, ClipFactory<*>>()

    init {
        registerClipFactory(MidiClipFactoryImpl())
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

    override fun <T : Clip> createTrackClip(clip: T): TrackClip<T> = TrackClipImpl(clip)
    override suspend fun createTrackClip(path: String, json: JsonNode) =
        TrackClipImpl(createClip(path, json["clip"]!!.asText()), json["time"]!!.asInt(), json["duration"]!!.asInt())
}