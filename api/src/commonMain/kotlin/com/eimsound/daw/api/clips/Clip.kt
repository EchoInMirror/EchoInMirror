package com.eimsound.daw.api.clips

import androidx.compose.ui.graphics.vector.ImageVector
import com.eimsound.daw.commons.json.*
import com.eimsound.daw.utils.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface Clip : JsonSerializable {
    val id: String
    val name: String
    val icon: ImageVector?
    val factory: ClipFactory<*>
    @Transient
    val defaultDuration: Int
    @Transient
    val isExpandable: Boolean
    @Transient
    val maxDuration: Int
    @Transient
    val duration: Int
}

abstract class AbstractClip<T: Clip>(override val factory: ClipFactory<T>) : Clip {
    override var id = randomId()
    override val name: String = ""
    override val isExpandable = false
    override val defaultDuration = -1
    override val maxDuration = -1
    override val duration get() = maxDuration
    override val icon: ImageVector? = null

    override fun toString(): String {
        return "MidiClipImpl(factory=$factory, id='$id')"
    }

    override fun fromJson(json: JsonElement) {
        json as JsonObject
        id = json["id"]!!.asString()
    }
}
