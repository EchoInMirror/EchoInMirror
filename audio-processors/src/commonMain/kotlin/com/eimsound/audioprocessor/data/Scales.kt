package com.eimsound.audioprocessor.data

data class Scale(val name: String, val scale: BooleanArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Scale) return false

        if (name != other.name) return false
        if (!scale.contentEquals(other.scale)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + scale.contentHashCode()
        return result
    }
}

val scales = arrayListOf(
    Scale("Major", booleanArrayOf(false, true, false, true, false, false, true, false, true, false, true, false))
)
val defaultScale = scales[0]
