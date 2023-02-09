package com.eimsound.daw.impl.clips

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.daw.api.Clip
import com.eimsound.daw.api.ClipFactory
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*

class ClipManagerImpl : ClipManager {
    override val factories: MutableMap<String, ClipFactory<*>> = mutableStateMapOf()

    init { reload() }

    override fun reload() {
        factories.clear()
        ServiceLoader.load(ClipFactory::class.java).forEach { factories[it.name] = it }
    }

    override suspend fun createClip(factory: String) =
        factories[factory]?.createClip() ?: throw NoSuchFactoryException(factory)

    override suspend fun createClip(path: String, json: JsonNode) =
        factories[json["factory"].asText()]?.createClip(path, json) ?: throw NoSuchFactoryException(json["factory"].asText())

    override suspend fun createClip(path: String, id: String) =
        createClip(path, ObjectMapper().readTree(File(path, "$id.json")))

    override fun <T : Clip> createTrackClip(clip: T, time: Int, duration: Int, start: Int, track: Track?) =
        TrackClipImpl(clip, time, duration, start, track)
    override suspend fun createTrackClip(path: String, json: JsonNode) = json["clip"]!!.let {
        TrackClipImpl(if (it.isObject) createClip(path, it) else createClip(path, it.asText()),
            json["time"]!!.asInt(), json["duration"]!!.asInt())
    }
}