@file:Suppress("PrivatePropertyName")

package com.eimsound.daw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Manifest
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name

val WORKING_PATH: Path = Path.of(
    if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support"
)
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")
@Suppress("MemberVisibilityCanBePrivate")
val AUDIO_THUMBNAIL_CACHE_PATH: Path = ROOT_PATH.resolve("audioThumbnailCache.db")
private val RECENT_PROJECT_PATH = ROOT_PATH.resolve("recentProjects.json")
private val CONFIG_PATH = ROOT_PATH.resolve("config.json")

var VERSION = "0.0.0"
    private set
var RELEASE_TIME: Long = System.currentTimeMillis()
    private set

object Configuration {
    @Suppress("MemberVisibilityCanBePrivate")
    var nativeHostPath: Path
    var stopAudioOutputOnBlur = false
    var audioDeviceFactoryName by mutableStateOf("")
    var audioDeviceName by mutableStateOf("")
    var autoCutOver0db by mutableStateOf(true)

    init {
        if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
        val resources = this::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
        while (resources.hasMoreElements()) {
            try {
                val manifest = Manifest(resources.nextElement().openStream())
                VERSION = manifest.mainAttributes.getValue("Implementation-Version")
                RELEASE_TIME = manifest.mainAttributes.getValue("Release-Time").toLong()
            } catch (ignored: Throwable) { }
        }
        if (CONFIG_PATH.exists()) load()
        nativeHostPath = Paths.get("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\MinSizeReel\\EIMHost.exe")
        if (!Files.exists(nativeHostPath)) nativeHostPath = Paths.get("EIMHost-x64.exe")
        val x86Host = nativeHostPath.absolute().parent.resolve(nativeHostPath.name.replace("x64", "x86"))

        System.setProperty("eim.dsp.nativeaudioplugins.list", ROOT_PATH.resolve("nativeAudioPlugins.json").absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host", nativeHostPath.absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host.x86", (if (Files.exists(x86Host)) x86Host else nativeHostPath).absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplayer.file", nativeHostPath.absolutePathString())
    }

    fun save() {
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(CONFIG_PATH.toFile(), this)
    }

    private fun load() {
        jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readerForUpdating(this).readValue<Configuration>(CONFIG_PATH.toFile())
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"

val recentProjects = mutableListOf<String>().apply {
    runCatching { jacksonObjectMapper().readValue<List<String>>(RECENT_PROJECT_PATH.toFile()) }.onSuccess { addAll(it) }
}

fun saveRecentProjects() {
    Files.write(RECENT_PROJECT_PATH, jacksonObjectMapper().writeValueAsBytes(recentProjects))
}
