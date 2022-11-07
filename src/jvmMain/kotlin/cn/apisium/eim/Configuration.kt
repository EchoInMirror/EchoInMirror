package cn.apisium.eim

import org.apache.commons.lang3.SystemUtils
import java.nio.file.Files
import java.nio.file.Path

val WORKING_PATH: Path = Path.of(if (SystemUtils.IS_OS_WINDOWS) System.getenv("AppData")
    else System.getProperty("user.home") + "/Library/Application Support")
val ROOT_PATH: Path = WORKING_PATH.resolve("EchoInMirror")

internal fun createDirectory() {
    if (!Files.exists(ROOT_PATH)) Files.createDirectory(ROOT_PATH)
}
