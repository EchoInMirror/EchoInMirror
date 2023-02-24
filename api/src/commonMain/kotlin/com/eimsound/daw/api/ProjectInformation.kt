package com.eimsound.daw.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
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

    fun save()
}

class DefaultProjectInformation(override val root: Path): ProjectInformation, JsonSerializable {
    override var name by mutableStateOf(root.name)
    override var author by mutableStateOf("")
    override var description by mutableStateOf("")
    override var timeCost by mutableStateOf(0)

    @Transient
    private val jsonFile = root.resolve("eim.json").toFile()

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
    }

    override fun save() { encodeJsonFile(jsonFile, true) }

    override fun toJson() = buildJsonObject {
        put("name", name)
        put("author", author)
        put("description", description)
        put("timeCost", timeCost)
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        name = json["name"]?.asString() ?: root.name
        author = json["author"]?.asString() ?: ""
        description = json["description"]?.asString() ?: ""
        timeCost = json["timeCost"]?.asInt() ?: 0
    }
}
