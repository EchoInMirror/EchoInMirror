@file:Suppress("NOTHING_TO_INLINE")

package com.eimsound.daw.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

inline fun JsonElement.asString() = jsonPrimitive.content
inline fun JsonElement.asInt() = jsonPrimitive.int
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
fun JsonSerializable.encodeJsonFile(file: File, prettier: Boolean = false) {
    val json = toJson()
    file.outputStream().use { (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToStream(json, it) }
}

fun JsonSerializable.fromJsonString(json: String) = fromJson(Json.parseToJsonElement(json))

fun JsonSerializable.fromJsonStream(stream: InputStream) {
    fromJson(Json.parseToJsonElement(InputStreamReader(stream).use { it.readText() }))
}

fun JsonSerializable.fromJsonFile(file: File) {
    fromJson(Json.parseToJsonElement(file.inputStream().use { it.reader().readText() }))
}

fun File.toJsonElement() = Json.parseToJsonElement(inputStream().use { it.reader().readText() })
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> File.toJson() = Json.decodeFromStream<T>(inputStream())
@OptIn(ExperimentalSerializationApi::class)
fun JsonElement.encodeToFile(file: File) = file.outputStream().use { Json.encodeToStream(this, it) }
