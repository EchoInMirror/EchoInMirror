@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.eimsound.daw.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

inline fun JsonElement.asString() = jsonPrimitive.content
inline fun JsonElement.asInt() = jsonPrimitive.int
inline fun JsonElement.asLong() = jsonPrimitive.long
inline fun JsonElement.asDouble() = jsonPrimitive.double
inline fun JsonElement.asBoolean() = jsonPrimitive.boolean
inline fun JsonElement.asFloat() = jsonPrimitive.float
val JsonIgnoreDefaults = Json { ignoreUnknownKeys = true; encodeDefaults = false }
@OptIn(ExperimentalSerializationApi::class)
val JsonPrettier = Json { ignoreUnknownKeys = true; encodeDefaults = false; prettyPrint = true; prettyPrintIndent = "  " }

interface JsonSerializable {
    fun toJson(): JsonElement
    fun fromJson(json: JsonElement)
}

fun JsonSerializable.toJsonString(prettier: Boolean = false) =
    (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToString(toJson())

@OptIn(ExperimentalSerializationApi::class)
fun JsonSerializable.encodeJsonStream(stream: OutputStream, prettier: Boolean = false) {
    val json = toJson()
    stream.use { (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToStream(json, it) }
}

@OptIn(ExperimentalSerializationApi::class)
fun JsonSerializable.encodeJsonFile(file: Path, prettier: Boolean = false) {
    val json = toJson()
    file.outputStream().use { (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToStream(json, it) }
}

fun JsonSerializable.fromJsonString(json: String) = fromJson(Json.parseToJsonElement(json))

fun JsonSerializable.fromJsonStream(stream: InputStream) {
    fromJson(Json.parseToJsonElement(InputStreamReader(stream).use { it.readText() }))
}

fun JsonSerializable.fromJsonFile(file: Path) {
    fromJson(Json.parseToJsonElement(file.inputStream().use { it.reader().readText() }))
}

fun Path.toJsonElement() = Json.parseToJsonElement(inputStream().use { it.reader().readText() })
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> File.toJson() = Json.decodeFromStream<T>(inputStream())
@OptIn(ExperimentalSerializationApi::class)
fun JsonElement.encodeToFile(file: File) = file.outputStream().use { Json.encodeToStream(this, it) }

inline fun JsonObjectBuilder.put(key: String, value: Collection<String>) { put(key, JsonArray(value.map { JsonPrimitive(it) })) }
@JvmName("putNotDefaultStringList")
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Collection<String>?) {
    if (!value.isNullOrEmpty()) put(key, JsonArray(value.map { JsonPrimitive(it) }))
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: List<JsonElement>?) {
    if (!value.isNullOrEmpty()) put(key, JsonArray(value))
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Collection<JsonSerializable>?) {
    if (!value.isNullOrEmpty()) put(key, JsonArray(value.map(JsonSerializable::toJson)))
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: JsonElement?) {
    if (value == null || (value is JsonArray && value.isEmpty()) || (value is JsonObject && value.isEmpty())) return
    put(key, value)
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: String?) { if (!value.isNullOrEmpty()) put(key, value) }
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Int?, defaultValue: Int = 0) {
    if (value != null && value != defaultValue) put(key, value)
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Boolean?) { if (value != null && value) put(key, value) }
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Double?) { if (value != null && value != 0.0) put(key, value) }
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Float?, defaultValue: Float = 0F) {
    if (value != null && value != defaultValue) put(key, value)
}
inline fun JsonObjectBuilder.putNotDefault(key: String, value: Long?) { if (value != null && value != 0L) put(key, value) }
