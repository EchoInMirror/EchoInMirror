package com.eimsound.daw

import com.eimsound.daw.utils.OBJECT_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Manifest

val WORKING_PATH: Path = Path.of(if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support")
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")
val NATIVE_AUDIO_PLUGIN_CONFIG: Path = ROOT_PATH.resolve("nativeAudioPlugin.json")
private val RECENT_PROJECT_PATH = ROOT_PATH.resolve("recentProjects.json")

internal fun createDirectories() {
    if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
}

var VERSION = "0.0.0"
    private set
var RELEASE_TIME: Long = System.currentTimeMillis()
    private set

object Configuration {
    var nativeHostPath: Path
    init {
        val resources = this::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
        while (resources.hasMoreElements()) {
            try {
                val manifest = Manifest(resources.nextElement().openStream())
                VERSION = manifest.mainAttributes.getValue("Implementation-Version")
                RELEASE_TIME = manifest.mainAttributes.getValue("Release-Time").toLong()
            } catch (ignored: Throwable) { }
        }
        nativeHostPath = Paths.get("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\MinSizeRel\\EIMHost.exe")
        if (!Files.exists(nativeHostPath)) nativeHostPath = Paths.get("EIMHost.exe")
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"

val recentProjects = mutableListOf<String>().apply {
    runCatching { OBJECT_MAPPER.readValue<List<String>>(RECENT_PROJECT_PATH.toFile()) }.onSuccess { addAll(it) }
}
fun saveRecentProjects() { Files.write(RECENT_PROJECT_PATH, OBJECT_MAPPER.writeValueAsBytes(recentProjects)) }
