package com.eimsound.dsp.timestretcher

import java.util.ServiceLoader

/**
 * @see com.eimsound.dsp.native.timestretcher.NativeTimeStretcherFactory
 */
interface TimeStretcherFactory {
    val timeStretchers: List<String>
    fun createTimeStretcher(name: String): TimeStretcher?
}

object TimeStretcherManager {
    private val factories by lazy { ServiceLoader.load(TimeStretcherFactory::class.java).toList() }
    val timeStretchers get() = factories.flatMap { it.timeStretchers }

    @Suppress("unused")
    fun createTimeStretcher() = timeStretchers.firstOrNull()?.let { createTimeStretcher(it) }
        ?: throw NoSuchElementException("No time stretchers found")
    fun createTimeStretcher(name: String): TimeStretcher? {
        factories.forEach { factory ->
            factory.createTimeStretcher(name)?.let { return it }
        }
        return null
    }
}
