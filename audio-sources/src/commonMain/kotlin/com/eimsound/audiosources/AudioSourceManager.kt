package com.eimsound.audiosources

import com.eimsound.daw.commons.NoSuchFactoryException
import com.eimsound.daw.commons.Reloadable
import com.eimsound.daw.commons.json.asString
import kotlinx.serialization.json.JsonObject
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.util.*

/**
 * @see com.eimsound.audiosources.AudioSourceManagerImpl
 */
interface AudioSourceManager : Reloadable {
    companion object {
        val instance = AudioSourceManagerImpl()
    }
    val factories: Map<String, AudioSourceFactory<*>>
    val supportedFormats: Set<String>
    val cachedFileSize: Int
    val fileSourcesCache: MutableMap<Path, WeakReference<MemoryAudioSource>>

    fun createAudioSource(factory: String, source: AudioSource? = null): AudioSource
    fun createAudioSource(json: JsonObject): AudioSource
    fun createAudioSource(file: Path, factory: String? = null): FileAudioSource
    fun createMemorySource(source: AudioSource, factory: String? = null): MemoryAudioSource
    fun createResampledSource(source: AudioSource, factory: String? = null): ResampledAudioSource
}

class AudioSourceManagerImpl : AudioSourceManager {
    override val cachedFileSize = 32 * 1024 * 1024 // 32MB
    override val fileSourcesCache = mutableMapOf<Path, WeakReference<MemoryAudioSource>>()
    override val factories = mutableMapOf<String, AudioSourceFactory<*>>()
    override val supportedFormats get() = factories.values.mapNotNull { it as? FileAudioSourceFactory<*> }
        .flatMap { it.supportedFormats }.toSet()

    init { reload() }

    override fun createAudioSource(factory: String, source: AudioSource?): AudioSource {
        return factories[factory]?.createAudioSource(source) ?: throw NoSuchFactoryException(factory)
    }

    override fun createAudioSource(json: JsonObject): AudioSource {
        val list = arrayListOf<JsonObject>()
        var node: JsonObject? = json
        while (node != null) {
            list.add(node)
            node = node["source"] as? JsonObject
        }
        var source: AudioSource? = null
        for (i in list.lastIndex downTo 0) {
            node = list[i]
            val factory = node["factory"]!!.asString()
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            source = f.createAudioSource(source, node)
        }
        return source ?: throw IllegalStateException("No source")
    }

    override fun createAudioSource(file: Path, factory: String?): FileAudioSource = if (factory != null) {
        val f = factories[factory] ?: throw NoSuchFactoryException(factory)
        if (f !is FileAudioSourceFactory<*>) throw UnsupportedOperationException("Factory $factory does not support files")
        f.createAudioSource(file)
    } else factories.firstNotNullOfOrNull { (_, value) ->
        if (value !is FileAudioSourceFactory<*>) return@firstNotNullOfOrNull null
        try {
            value.createAudioSource(file)
        } catch (e: Throwable) {
            throw UnsupportedOperationException("No factory supports file $file", e)
        }
    } ?: throw UnsupportedOperationException("No factory supports file $file")

    override fun createResampledSource(source: AudioSource, factory: String?) =
        createAudioSource<ResampledAudioSource, ResampledAudioSourceFactory<ResampledAudioSource>>(source, factory)

    override fun createMemorySource(source: AudioSource, factory: String?) =
        createAudioSource<MemoryAudioSource, MemoryAudioSourceFactory<MemoryAudioSource>>(source, factory)

    private inline fun <reified A: AudioSource, reified T: AudioSourceFactory<A>> createAudioSource(source: AudioSource, factory: String?): A {
        if (factory != null) {
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            if (f !is T) throw UnsupportedOperationException("Factory $factory does not inherited from ${T::class.simpleName}")
            return f.createAudioSource(source)
        }
        return factories.firstNotNullOfOrNull { (_, value) ->
            if (value !is T) return@firstNotNullOfOrNull null
            try {
                value.createAudioSource(source)
            } catch (e: Throwable) {
                throw UnsupportedOperationException("No factory from $source", e)
            }
        } ?: throw UnsupportedOperationException("No factory inherited from ${T::class.simpleName}")
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(AudioSourceFactory::class.java).associateBy { it.name })
    }
}
