package com.eimsound.audiosources

import com.eimsound.audiosources.impl.ProxyFileAudioSource
import com.eimsound.audiosources.impl.DefaultMemoryAudioSource
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.util.*

class NoSuchAudioSourceFactoryException(name: String?) : Exception("No audio source factory with name $name")

@Suppress("MemberVisibilityCanBePrivate")
object AudioSourceManager {
    var cachedFileSize = 32 * 1024 * 1024 // 32MB
    var maxNotRandomAccessableFileCacheSize = 500 * 1024 * 1024 // 500MB
    val fileSourcesCache = mutableMapOf<Path, WeakReference<MemoryAudioSource>>()
    val fileAudioSourceFactories = mutableMapOf<String, FileAudioSourceFactory>()
    val resampledAudioSourceFactories = mutableMapOf<String, ResampledAudioSourceFactory>()
    private val _supportedFormats = mutableSetOf<String>()
    val supportedFormats: Set<String> get() = _supportedFormats

    init { reload() }

    fun createFileAudioSource(file: Path, factory: String? = null): FileAudioSource = if (factory != null) {
        (fileAudioSourceFactories[factory] ?: throw NoSuchAudioSourceFactoryException(factory)).createAudioSource(file)
    } else fileAudioSourceFactories.firstNotNullOfOrNull { (_, value) ->
        try {
            value.createAudioSource(file)
        } catch (e: Throwable) {
            throw UnsupportedOperationException("No factory supports file $file", e)
        }
    } ?: throw UnsupportedOperationException("No factory supports file $file")

    fun createResampledSource(source: AudioSource, factory: String? = null) =
        (resampledAudioSourceFactories[factory] ?: resampledAudioSourceFactories.values.lastOrNull() ?:
            throw NoSuchAudioSourceFactoryException(factory)).createAudioSource(source)

    fun createMemorySource(source: AudioSource) = DefaultMemoryAudioSource(source)

    fun createCachedFileSource(file: Path, factory: String? = null): FileAudioSource {
        var cached = synchronized(fileSourcesCache) { fileSourcesCache[file]?.get() }
        if (cached == null) {
            val source = createFileAudioSource(file, factory)

            val fileSize = source.fileSize
            if ((cachedFileSize > 0 && fileSize < cachedFileSize) ||
                (!source.isRandomAccessible && fileSize < maxNotRandomAccessableFileCacheSize)) {
                cached = createMemorySource(source)
                synchronized(fileSourcesCache) { fileSourcesCache[file] = WeakReference(cached) }
            } else return source
        }
        return ProxyFileAudioSource(file, cached)
    }

    fun createProxyFileSource(file: Path, factory: String? = null): FileAudioSource {
        val cached = synchronized(fileSourcesCache) { fileSourcesCache[file]?.get() }
        if (cached != null) return ProxyFileAudioSource(file, cached)
        val source = createFileAudioSource(file, factory)
        if (!source.isRandomAccessible && source.fileSize < maxNotRandomAccessableFileCacheSize)
            return ProxyFileAudioSource(file, createMemorySource(source))
        return source
    }

    fun reload() {
        _supportedFormats.clear()
        fileAudioSourceFactories.clear()
        fileAudioSourceFactories.putAll(ServiceLoader.load(FileAudioSourceFactory::class.java).associateBy { it.name })
        fileAudioSourceFactories.values.forEach { it.supportedFormats.forEach(_supportedFormats::add) }
        resampledAudioSourceFactories.clear()
        resampledAudioSourceFactories.putAll(ServiceLoader.load(ResampledAudioSourceFactory::class.java).associateBy { it.name })
    }
}
