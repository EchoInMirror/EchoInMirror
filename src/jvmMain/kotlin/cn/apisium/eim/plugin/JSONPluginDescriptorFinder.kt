package cn.apisium.eim.plugin

import kotlinx.serialization.json.*
import org.pf4j.*
import org.pf4j.util.FileUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipFile

private const val PLUGIN_ID = "id"
private const val PLUGIN_DESCRIPTION = "description"
private const val PLUGIN_CLASS = "class"
private const val PLUGIN_VERSION = "version"
private const val PLUGIN_PROVIDER = "provider"
private const val PLUGIN_DEPENDENCIES = "dependencies"
private const val PLUGIN_REQUIRES = "requires"
private const val PLUGIN_LICENSE = "license"

class EIMPluginDescriptor(pluginId: String, pluginDescription: String?, pluginClass: String?, version: String?,
                          requires: String?, provider: String?, license: String?, dependencies: String?):
    DefaultPluginDescriptor(pluginId, pluginDescription, pluginClass, version, requires ?: "*", provider ?: "", license ?: "") {
        init { if (!dependencies.isNullOrEmpty()) setDependencies(dependencies) }
    }

class JSONPluginDescriptorFinder: PluginDescriptorFinder {
    private val log = LoggerFactory.getLogger(JSONPluginDescriptorFinder::class.java)

    override fun isApplicable(pluginPath: Path) = Files.exists(pluginPath) &&
            (Files.isDirectory(pluginPath) || FileUtils.isZipOrJarFile(pluginPath))

    override fun find(pluginPath: Path) = createPluginDescriptor(
        if (FileUtils.isJarFile(pluginPath)) readManifestFromJar(pluginPath)
        else if (FileUtils.isZipFile(pluginPath)) readManifestFromZip(pluginPath)
        else readManifestFromDirectory(pluginPath)
    )

    private fun createPluginDescriptor(json: JsonObject) = EIMPluginDescriptor(
        json[PLUGIN_ID]!!.jsonPrimitive.content,
        json[PLUGIN_DESCRIPTION]?.jsonPrimitive?.content,
        json[PLUGIN_CLASS]?.jsonPrimitive?.content,
        json[PLUGIN_VERSION]?.jsonPrimitive?.content,
        json[PLUGIN_REQUIRES]?.jsonPrimitive?.content,
        json[PLUGIN_PROVIDER]?.jsonPrimitive?.content,
        json[PLUGIN_LICENSE]?.jsonPrimitive?.content,
        json[PLUGIN_DEPENDENCIES]?.jsonPrimitive?.content
    )

    private fun readFromStream(stream: InputStream) = Json.parseToJsonElement(stream.reader().readText())

    private fun readManifestFromJar(jarPath: Path) = try {
        JarFile(jarPath.toFile()).use { readFromStream(it.getInputStream(it.getJarEntry("plugin.json"))).jsonObject }
    } catch (e: IOException) {
        throw PluginRuntimeException(e, "Cannot read manifest from {}", jarPath)
    }

    private fun readManifestFromZip(zipPath: Path) = try {
        ZipFile(zipPath.toFile()).use { readFromStream(it.getInputStream(it.getEntry("plugin.json"))).jsonObject }
    } catch (e: IOException) {
        throw PluginRuntimeException(e, "Cannot read manifest from {}", zipPath)
    }

    private fun readManifestFromDirectory(pluginPath: Path?): JsonObject {
        // legacy (the path is something like "classes/META-INF/MANIFEST.MF")
        val json = FileUtils.findFile(pluginPath, "plugin.json")
            ?: throw PluginRuntimeException("Cannot find the manifest path")
        log.debug("Lookup plugin descriptor in '{}'", json)
        if (Files.notExists(json)) {
            throw PluginRuntimeException("Cannot find '{}' path", json)
        }
        try {
            return Files.newInputStream(json).use(::readFromStream).jsonObject
        } catch (e: IOException) {
            throw PluginRuntimeException(e, "Cannot read manifest from {}", pluginPath)
        }
    }
}