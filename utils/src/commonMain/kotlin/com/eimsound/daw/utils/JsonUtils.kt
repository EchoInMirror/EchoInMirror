package com.eimsound.daw.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

fun JsonElement?.asString() = this!!.jsonPrimitive.content
fun JsonElement.asString() = jsonPrimitive.content
fun JsonElement?.asInt() = this!!.jsonPrimitive.int
fun JsonElement.asInt() = jsonPrimitive.int
val JsonIgnoreDefaults = Json { ignoreUnknownKeys = true; encodeDefaults = false }
@OptIn(ExperimentalSerializationApi::class)
val JsonPrettier = Json { ignoreUnknownKeys = true; encodeDefaults = false; prettyPrint = true; prettyPrintIndent = "  " }

typealias JsonObjectSerializable = JsonSerializable<Map<String, Any>>
typealias JsonMutableObjectSerializable = JsonSerializable<MutableMap<String, Any>>
interface JsonSerializable<T : Any> {
    fun toJson(): T
    fun fromJson(json: JsonElement)
}

inline fun <reified T : Any> JsonSerializable<T>.toJsonString(prettier: Boolean = false) =
    (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToString(toJson())

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> JsonSerializable<T>.encodeJsonStream(stream: OutputStream, prettier: Boolean = false) {
    val json = toJson()
    stream.use { (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToStream(json, it) }
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T : Any> JsonSerializable<T>.encodeJsonFile(file: File, prettier: Boolean = false) {
    val json = toJson()
    withContext(Dispatchers.IO) {
        file.outputStream().use { (if (prettier) JsonPrettier else JsonIgnoreDefaults).encodeToStream(json, it) }
    }
}

fun <T: Any> JsonSerializable<T>.fromJsonString(json: String) = fromJson(Json.parseToJsonElement(json))

fun <T: Any> JsonSerializable<T>.fromJsonStream(stream: InputStream) {
    fromJson(Json.parseToJsonElement(InputStreamReader(stream).use { it.readText() }))
}

suspend fun <T: Any> JsonSerializable<T>.fromJsonFile(file: File) {
    fromJson(Json.parseToJsonElement(withContext(Dispatchers.IO) {
        file.inputStream().use { it.reader().readText() }
    }))
}

suspend fun File.toJsonElement() = Json.parseToJsonElement(withContext(Dispatchers.IO) {
    inputStream().use { it.reader().readText() }
})
