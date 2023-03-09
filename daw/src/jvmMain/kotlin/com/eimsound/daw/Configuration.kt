@file:Suppress("PrivatePropertyName")

package com.eimsound.daw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.SystemUtils
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.jar.Manifest
import java.util.logging.LogManager
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name

val WORKING_PATH: Path = Path.of(
    if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support"
)
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")
val LOGS_PATH: Path = ROOT_PATH.resolve("logs")
val FAVORITE_AUDIO_PROCESSORS_PATH: Path = ROOT_PATH.resolve("favoriteAudioProcessors.json")
@Suppress("MemberVisibilityCanBePrivate")
val AUDIO_THUMBNAIL_CACHE_PATH: Path = ROOT_PATH.resolve("audioThumbnailCache.db")
private val RECENT_PROJECT_PATH = ROOT_PATH.resolve("recentProjects.json")
private val CONFIG_PATH = ROOT_PATH.resolve("config.json")

var VERSION = "0.0.0"
    private set
var RELEASE_TIME: Long = System.currentTimeMillis()
    private set
internal var APP_CENTER_SECRET = ""
    private set

object Configuration : JsonSerializable {
    @Suppress("MemberVisibilityCanBePrivate")
    var nativeHostPath: Path
    var stopAudioOutputOnBlur = false
    var audioDeviceFactoryName by mutableStateOf("")
    var audioDeviceName by mutableStateOf("")
    var autoCutOver0db by mutableStateOf(true)
    var userId = UUID.randomUUID().toString()
        private set

    init {
        if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
        LogManager.getLogManager().readConfiguration(IOUtils.toInputStream(
            "handlers=com.eimsound.daw.dawutils.FileLogHandler, java.util.logging.ConsoleHandler\n" +
                "java.util.logging.ConsoleHandler.formatter=com.eimsound.daw.dawutils.LogFormatter", Charset.defaultCharset()
        ))
        if (!Files.exists(LOGS_PATH)) Files.createDirectory(LOGS_PATH)
        val resources = this::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
        while (resources.hasMoreElements()) try {
            val e = resources.nextElement()
            val m = Manifest(e.openStream()).mainAttributes
            if (m.getValue("Name") != "EchoInMirror") continue
            m.apply {
                VERSION = getValue("Implementation-Version")
                if (containsKey("Release-Time")) RELEASE_TIME = getValue("Release-Time").toLong()
                APP_CENTER_SECRET = (if (containsKey("App-Center-Secret")) getValue("App-Center-Secret")
                else System.getenv("APP_CENTER_SECRET")) ?: ""
            }
            break
        } catch (ignored: Throwable) { }
        if (CONFIG_PATH.exists()) fromJsonFile(CONFIG_PATH.toFile())
        else save()
        nativeHostPath = Paths.get("D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\MinSizeRel\\EIMHost.exe")
        if (!Files.exists(nativeHostPath)) nativeHostPath = Paths.get("EIMHost-x64.exe")
        val x86Host = nativeHostPath.absolute().parent.resolve(nativeHostPath.name.replace("x64", "x86"))

        System.setProperty("com.microsoft.appcenter.crashes.uncaughtexception.autosend", "true")
        System.setProperty("com.microsoft.appcenter.preferences", ROOT_PATH.resolve("appCenterPreferences.json").absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.list", ROOT_PATH.resolve("nativeAudioPlugins.json").absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host", nativeHostPath.absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host.x86", (if (Files.exists(x86Host)) x86Host else nativeHostPath).absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplayer.file", nativeHostPath.absolutePathString())
    }

    fun save() { encodeJsonFile(CONFIG_PATH.toFile(), true) }

    override fun toJson() = buildJsonObject {
        put("userId", userId)
        put("stopAudioOutputOnBlur", stopAudioOutputOnBlur)
        put("audioDeviceFactoryName", audioDeviceFactoryName)
        put("audioDeviceName", audioDeviceName)
        put("autoCutOver0db", autoCutOver0db)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["stopAudioOutputOnBlur"]?.asBoolean()?.let { stopAudioOutputOnBlur = it }
        json["audioDeviceFactoryName"]?.asString()?.let { audioDeviceFactoryName = it }
        json["audioDeviceName"]?.asString()?.let { audioDeviceName = it }
        json["autoCutOver0db"]?.asBoolean()?.let { autoCutOver0db = it }
        val id = json["userId"]?.asString()
        if (id == null) save() else userId = id
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"

val recentProjects = mutableStateListOf<String>().apply {
    runCatching { runBlocking { RECENT_PROJECT_PATH.toFile().toJson<List<String>>() } }.onSuccess(::addAll)
}

@OptIn(ExperimentalSerializationApi::class)
fun saveRecentProjects() {
    if (recentProjects.size > 20) recentProjects.subList(20, recentProjects.size).clear()
    Json.encodeToStream<List<String>>(recentProjects, RECENT_PROJECT_PATH.toFile().outputStream())
}
