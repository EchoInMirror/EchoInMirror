package com.eimsound.daw.impl.clips

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.daw.api.Clip
import com.eimsound.daw.api.ClipFactory
import com.eimsound.daw.api.ClipManager
import com.eimsound.daw.api.processor.Track
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.asInt
import com.eimsound.daw.utils.asString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonObject
import java.util.*

private val logger = KotlinLogging.logger {  }
class ClipManagerImpl : ClipManager {
    override val factories: MutableMap<String, ClipFactory<*>> = mutableStateMapOf()

    init { reload() }

    override fun reload() {
        factories.clear()
        ServiceLoader.load(ClipFactory::class.java).forEach { factories[it.name] = it }
    }

    override suspend fun createClip(factory: String) =
        factories[factory]?.createClip() ?: throw NoSuchFactoryException(factory)

    override suspend fun createClip(path: String, json: JsonObject): Clip {
        val name = json["factory"]?.asString()
        logger.info { "Creating clip ${json["id"]} in $path with factory \"$name\"" }
        return factories[name]?.createClip(path, json) ?: throw NoSuchFactoryException(name ?: "Null")
    }

    override fun <T : Clip> createTrackClip(clip: T, time: Int, duration: Int, start: Int, track: Track?) =
        TrackClipImpl(clip, time, duration, start, track)
    override suspend fun createTrackClip(path: String, json: JsonObject) =
        TrackClipImpl(createClip(path, json["clip"] as JsonObject), json["time"]?.asInt() ?: 0,
            json["duration"]?.asInt() ?: 0)
}
