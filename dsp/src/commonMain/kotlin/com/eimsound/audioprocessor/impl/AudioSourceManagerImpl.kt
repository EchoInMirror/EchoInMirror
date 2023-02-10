package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.fasterxml.jackson.databind.JsonNode
import java.io.File
import java.util.*

class AudioSourceManagerImpl : AudioSourceManager {
    override val factories = mutableStateMapOf<String, AudioSourceFactory<*>>()
    override val supportedFormats get() = factories.values.mapNotNull { it as? FileAudioSourceFactory<*> }
        .flatMap { it.supportedFormats }.toSet()

    init { reload() }

    override fun createAudioSource(factory: String, source: AudioSource?): AudioSource {
        return factories[factory]?.createAudioSource(source) ?: throw NoSuchFactoryException(factory)
    }

    override fun createAudioSource(json: JsonNode): AudioSource {
        val list = arrayListOf<JsonNode>()
        var node = json
        while (node.has("source")) {
            list.add(node)
            node = node["source"]
        }
        var source: AudioSource? = null
        for (i in list.lastIndex downTo 0) {
            node = list[i]
            val factory = node["factory"].asText()
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            source = f.createAudioSource(source, node)
        }
        return source ?: throw IllegalStateException("No source")
    }

    override fun createAudioSource(file: File, factory: String?): FileAudioSource {
        if (factory != null) {
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            if (f !is FileAudioSourceFactory<*>) throw UnsupportedOperationException("Factory $factory does not support files")
            return f.createAudioSource(file)
        }
        return factories.firstNotNullOfOrNull { (_, value) ->
            if (value !is FileAudioSourceFactory<*>) return@firstNotNullOfOrNull null
            try {
                value.createAudioSource(file)
            } catch (ignored: UnsupportedOperationException) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } ?: throw UnsupportedOperationException("No factory supports file $file")
    }

    override fun createResampledSource(source: AudioSource, factory: String?): ResampledAudioSource {
        if (factory != null) {
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            if (f !is ResampledSourceFactory<*>) throw UnsupportedOperationException("Factory $factory does not support resampling")
            return f.createAudioSource(source)
        }
        return factories.firstNotNullOfOrNull { (_, value) ->
            if (value !is ResampledSourceFactory<*>) return@firstNotNullOfOrNull null
            try {
                value.createAudioSource(source)
            } catch (ignored: UnsupportedOperationException) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } ?: throw UnsupportedOperationException("No factory supports resampling")
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(AudioSourceFactory::class.java).associateBy { it.name })
    }
}