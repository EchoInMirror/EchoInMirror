package cn.apisium.eim

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path

val WORKING_PATH: Path = Path.of(if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support")
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")

internal fun createDirectories() {
    if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
}

@Serializable
object Configuration {
    var nativeHostPath: String
    init {
        nativeHostPath = "D:\\Cpp\\EIMPluginScanner\\build\\EIMHost_artefacts\\MinSizeRel\\EIMHost.exe"
        if (!Files.exists(Path.of(nativeHostPath))) nativeHostPath = "EIMHost.exe"
    }
}

val IS_DEBUG = System.getProperty("cn.apisium.eim.debug") == "true"
