package com.eimsound.daw.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * Consider to make this class open.
 * @see [DefaultProjectInformation]
 */
interface ProjectInformation {
    val root: Path
    var name: String
    var author: String
    var description: String
    var timeCost: Int
    val saved: Boolean

    fun save(file: Path? = null)
}

class DefaultProjectInformation(override val root: Path): ProjectInformation, JsonSerializable {
    override var name by mutableStateOf(root.name)
    override var author by mutableStateOf("")
    override var description by mutableStateOf("")
    override var timeCost by mutableStateOf(0)
    override var saved by mutableStateOf(true)
        private set

    @Transient
    private val jsonFile = root.resolve("eim.json")

    init {
        var flag = false
        if (jsonFile.exists()) {
            try {
                fromJsonFile(jsonFile)
                flag = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (!flag) save()

        EchoInMirror.undoManager.cursorChangeHandlers += { if (saved) saved = false }
    }

    override fun save(file: Path?) {
        encodeJsonFile(file?.resolve("eim.json") ?: jsonFile, true)
        saved = true
    }

    override fun toJson() = buildJsonObject {
        putNotDefault("name", name)
        putNotDefault("author", author)
        putNotDefault("description", description)
        putNotDefault("timeCost", timeCost)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        name = json["name"]?.asString() ?: root.name
        author = json["author"]?.asString() ?: ""
        description = json["description"]?.asString() ?: ""
        timeCost = json["timeCost"]?.asInt() ?: 0
    }

    override fun toString(): String {
        return "DefaultProjectInformation(root=$root, name='$name', author='$author', description='$description', timeCost=$timeCost)"
    }
}
