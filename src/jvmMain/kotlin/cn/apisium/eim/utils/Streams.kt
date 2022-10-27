package cn.apisium.eim.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

class EIMInputStream(private val isBigEndian: Boolean, stream: InputStream): BufferedInputStream(stream) {
    fun readInt() = if (isBigEndian) (read() shl 24) or (read() shl 16) or (read() shl 8) or read()
        else read() or (read() shl 8) or (read() shl 16) or (read() shl 24)
    fun readFloat() = Float.fromBits(readInt())
    fun readString(): String {
        val arr = ByteArray(readInt())
        read(arr)
        return arr.toString(Charsets.UTF_8)
    }
}

class EIMOutputStream(private val isBigEndian: Boolean, stream: OutputStream): BufferedOutputStream(stream) {
    private val writeBuffer = ByteArray(8)
    fun writeInt(value: Int) {
        if (isBigEndian) {
            writeBuffer[0] = (value ushr 24).toByte()
            writeBuffer[1] = (value ushr 16).toByte()
            writeBuffer[2] = (value ushr 8).toByte()
            writeBuffer[3] = value.toByte()
        } else {
            writeBuffer[3] = (value ushr 24).toByte()
            writeBuffer[2] = (value ushr 16).toByte()
            writeBuffer[1] = (value ushr 8).toByte()
            writeBuffer[0] = value.toByte()
        }
        write(writeBuffer, 0, 4)
    }
    fun writeLong(value: Long) {
        if (isBigEndian) {
            writeBuffer[0] = (value ushr 56).toByte()
            writeBuffer[1] = (value ushr 48).toByte()
            writeBuffer[2] = (value ushr 40).toByte()
            writeBuffer[3] = (value ushr 32).toByte()
            writeBuffer[4] = (value ushr 24).toByte()
            writeBuffer[5] = (value ushr 16).toByte()
            writeBuffer[6] = (value ushr 8).toByte()
            writeBuffer[7] = value.toByte()
        } else {
            writeBuffer[7] = (value ushr 56).toByte()
            writeBuffer[6] = (value ushr 48).toByte()
            writeBuffer[5] = (value ushr 40).toByte()
            writeBuffer[4] = (value ushr 32).toByte()
            writeBuffer[3] = (value ushr 24).toByte()
            writeBuffer[2] = (value ushr 16).toByte()
            writeBuffer[1] = (value ushr 8).toByte()
            writeBuffer[0] = value.toByte()
        }
        write(writeBuffer, 0, 8)
    }
    fun writeFloat(value: Float) = writeInt(java.lang.Float.floatToIntBits(value))
    fun writeDouble(value: Double) = writeLong(java.lang.Double.doubleToLongBits(value))
}
