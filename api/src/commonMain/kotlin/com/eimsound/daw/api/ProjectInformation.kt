package com.eimsound.daw.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eimsound.daw.utils.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*
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

    suspend fun save()
}

class DefaultProjectInformation(override val root: Path): ProjectInformation, JsonObjectSerializable {
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
                runBlocking { fromJsonFile(jsonFile) }
                flag = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (!flag) runBlocking { save() }
    }

    override suspend fun save() { encodeJsonFile(jsonFile, true) }

    override fun toJson() = mapOf(
        "name" to name,
        "author" to author,
        "description" to description,
        "timeCost" to timeCost,
    )

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        name = json["name"]?.asString() ?: root.name
        author = json["author"]?.asString() ?: ""
        description = json["description"]?.asString() ?: ""
        timeCost = json["timeCost"]?.asInt() ?: 0
    }
}
