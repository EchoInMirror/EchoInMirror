package com.eimsound.daw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.commons.json.*
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.daw.utils.observableMutableStateOf
import com.jthemedetecor.OsThemeDetector
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.jar.Manifest
import kotlin.io.path.*

val WORKING_PATH = Path(
    if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else if(SystemUtils.IS_OS_MAC) System.getProperty("user.home") + "/Library/Application Support"
    else if(SystemUtils.IS_OS_LINUX) System.getenv("HOME") + "/.local"
    else throw UnsupportedOperationException("Unsupported operating system!")
)
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")
val FAVORITE_AUDIO_PROCESSORS_PATH: Path = ROOT_PATH.resolve("favoriteAudioProcessors.json")
val AUDIO_THUMBNAIL_CACHE_PATH: Path = ROOT_PATH.resolve("audioThumbnailCaches")
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
    var preferredSampleRate by mutableStateOf(-1)
    var preferredBufferSize by mutableStateOf(-1)
    var autoCutOver0db by mutableStateOf(true)
    var isTimeDisplayInBeats by mutableStateOf(false)
    var themeMode by observableMutableStateOf(2) {
        if (it == 0 || it == 1) EchoInMirror.windowManager.isDarkTheme = it == 1
        else EchoInMirror.windowManager.isDarkTheme = themeDetector.isDark
    }
    val fileBrowserCustomRoots = mutableStateSetOf<Path>()
    var fileBrowserShowSupFormatOnly by mutableStateOf(true)
    var userId = UUID.randomUUID().toString()
        private set

    private val themeDetector = OsThemeDetector.getDetector().apply {
        registerListener { EchoInMirror.windowManager.isDarkTheme = it }
    }

    init {
        if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
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
        if (CONFIG_PATH.exists()) fromJsonFile(CONFIG_PATH)
        else save()
        nativeHostPath = Path(if (SystemUtils.IS_OS_MAC)
            "/Users/shirasawa/code/EIMHost/cmake-build-debug/EIMHost_artefacts/Debug/EIMHost.app/Contents/MacOS/EIMHost"
            else "D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\Debug\\EIMHost.exe")
        if (!Files.exists(nativeHostPath)) {
            nativeHostPath = Path(
                if (SystemUtils.IS_OS_WINDOWS) "EIMHost.exe"
                else if (SystemUtils.IS_OS_MAC) "EIMHost.app/Contents/MacOS/EIMHost"
                else "EIMHost"
            )
        }
        var execExt = ""
        val x86Host = if (SystemUtils.IS_OS_WINDOWS) {
            execExt = ".exe"
            nativeHostPath.absolute().parent.resolve(nativeHostPath.name.removeSuffix(".exe") + "-x86.exe")
        } else nativeHostPath

        System.setProperty("com.microsoft.appcenter.crashes.uncaughtexception.autosend", "true")
        System.setProperty("com.microsoft.appcenter.preferences", ROOT_PATH.resolve("appCenterPreferences.json").absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.list", ROOT_PATH.resolve("nativeAudioPlugins.json").absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host", nativeHostPath.absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplugins.host.x86", (if (Files.exists(x86Host)) x86Host else nativeHostPath).absolutePathString())
        System.setProperty("eim.dsp.nativeaudioplayer.file", nativeHostPath.absolutePathString())
        System.setProperty("eim.eimutils.file", Path("EIMUtils/EIMUtils$execExt").absolutePathString())
        System.setProperty("eim.tempfiles.prefix", "EchoInMirror")

        val libraryExt = if (SystemUtils.IS_OS_WINDOWS) "dll" else if (SystemUtils.IS_OS_MAC) "dylib" else "so"
        var timeStretcherLibraryFile = ROOT_PATH.resolve("/Users/shirasawa/code/EIMTimeStretchers/cmake-build-debug/libEIMTimeStretchers.$libraryExt")
        if (!timeStretcherLibraryFile.exists()) timeStretcherLibraryFile = Path("libEIMTimeStretchers.$libraryExt")
        System.setProperty("eim.dsp.timestretchers.library.file", timeStretcherLibraryFile.absolutePathString())
    }

    fun save() { encodeJsonFile(CONFIG_PATH, true) }

    override fun toJson() = buildJsonObject {
        put("userId", userId)
        put("stopAudioOutputOnBlur", stopAudioOutputOnBlur)
        put("audioDeviceFactoryName", audioDeviceFactoryName)
        put("audioDeviceName", audioDeviceName)
        put("preferredSampleRate", preferredSampleRate)
        put("preferredBufferSize", preferredBufferSize)
        put("autoCutOver0db", autoCutOver0db)
        put("themeMode", themeMode)
        put("isTimeDisplayInBeats", isTimeDisplayInBeats)
        put("fileBrowserCustomRoots", Json.encodeToJsonElement(fileBrowserCustomRoots.map { it.pathString }))
        put("fileBrowserShowSupFormatOnly", fileBrowserShowSupFormatOnly)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        json["stopAudioOutputOnBlur"]?.asBoolean()?.let { stopAudioOutputOnBlur = it }
        json["audioDeviceFactoryName"]?.asString()?.let { audioDeviceFactoryName = it }
        json["audioDeviceName"]?.asString()?.let { audioDeviceName = it }
        json["preferredSampleRate"]?.asInt()?.let { preferredSampleRate = it }
        json["preferredBufferSize"]?.asInt()?.let { preferredBufferSize = it }
        json["autoCutOver0db"]?.asBoolean()?.let { autoCutOver0db = it }
        json["themeMode"]?.asInt()?.let {
            themeMode = it
            if (it == 0 || it == 1) EchoInMirror.windowManager.isDarkTheme = it == 1
            else EchoInMirror.windowManager.isDarkTheme = themeDetector.isDark
        }
        json["isTimeDisplayInBeats"]?.asBoolean()?.let { isTimeDisplayInBeats = it }
        fileBrowserCustomRoots.clear()
        (json["fileBrowserCustomRoots"] as? JsonArray)?.let {
            fileBrowserCustomRoots.addAll(it.fastMap { p -> Path(p.asString()) })
        }
        json["fileBrowserShowSupFormatOnly"]?.asBoolean()?.let { fileBrowserShowSupFormatOnly = it }
        val id = json["userId"]?.asString()
        if (id == null) save() else userId = id
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"

val recentProjects = mutableStateListOf<String>().apply {
    runCatching { RECENT_PROJECT_PATH.toFile().toJson<List<String>>() }.onSuccess(::addAll)
}

@OptIn(ExperimentalSerializationApi::class)
fun saveRecentProjects() {
    if (recentProjects.size > 20) recentProjects.subList(20, recentProjects.size).clear()
    Json.encodeToStream<List<String>>(recentProjects, RECENT_PROJECT_PATH.toFile().outputStream())
}
