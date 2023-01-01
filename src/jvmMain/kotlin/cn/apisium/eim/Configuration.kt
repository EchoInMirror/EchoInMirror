package cn.apisium.eim

import cn.apisium.eim.utils.OBJECT_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path

val WORKING_PATH: Path = Path.of(if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support")
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")
private val RECENT_PROJECT_PATH = ROOT_PATH.resolve("recentProjects.json")

internal fun createDirectories() {
    if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
}

object Configuration {
    var nativeHostPath: String
    init {
        nativeHostPath = "D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\MinSizeRel\\EIMHost.exe"
        if (!Files.exists(Path.of(nativeHostPath))) nativeHostPath = "EIMHost.exe"
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"
val VERSION = ""::class.java.getPackage().specificationVersion

val recentProjects = mutableListOf<String>().apply {
    runCatching { OBJECT_MAPPER.readValue<List<String>>(RECENT_PROJECT_PATH.toFile()) }.onSuccess { addAll(it) }
}
fun saveRecentProjects() { Files.write(RECENT_PROJECT_PATH, OBJECT_MAPPER.writeValueAsBytes(recentProjects)) }
