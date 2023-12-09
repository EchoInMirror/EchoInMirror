@file:Suppress("unused")

package com.eimsound.daw.utils

import java.io.*
import javax.sound.sampled.AudioInputStream

class ByteBufInputStream(private val isBigEndian: Boolean, stream: InputStream): BufferedInputStream(stream) {
    fun readInt() = if (isBigEndian) (read() shl 24) or (read() shl 16) or (read() shl 8) or read()
        else read() or (read() shl 8) or (read() shl 16) or (read() shl 24)
    fun readFloat() = Float.fromBits(readInt())
    fun readString(): String {
        val len = readVarInt()
        if (len == 0) return ""
        val arr = ByteArray(len)
        read(arr)
        return arr.toString(Charsets.UTF_8)
    }
    fun readBoolean() = read() != 0
    fun readShort() = if (isBigEndian) (read() shl 8) or read() else read() or (read() shl 8)
    fun readVarInt(): Int {
        var result = 0
        for (shift in 0..28 step 7) {
            val b = read()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
        }
        throw IOException("Invalid var int")
    }
    fun readVarLong(): Long {
        var result = 0L
        for (shift in 0..63 step 7) {
            val b = read()
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
        }
        throw IOException("Invalid var long")
    }
    fun readStringArray() = Array(readVarInt()) { readString() }
}

class ByteBufOutputStream(private val isBigEndian: Boolean, stream: OutputStream): BufferedOutputStream(stream) {
    private val writeBuffer = ByteArray(8)
    fun writeShort(value: Short) {
        if (isBigEndian) {
            writeBuffer[0] = (value.toInt() ushr 8).toByte()
            writeBuffer[1] = value.toByte()
        } else {
            writeBuffer[1] = (value.toInt() ushr 8).toByte()
            writeBuffer[0] = value.toByte()
        }
        write(writeBuffer, 0, 2)
    }
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
    @Suppress("MemberVisibilityCanBePrivate")
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
    fun writeBoolean(value: Boolean) = write(if (value) 1 else 0)
    fun writeString(value: String) {
        if (value.isEmpty()) {
            writeVarInt(0)
            return
        }
        val arr = value.toByteArray(Charsets.UTF_8)
        writeVarInt(arr.size)
        write(arr)
    }
    fun writeVarInt(value: Int) {
        var v = value
        while (v >= 0x80) {
            write(v and 0x7F or 0x80)
            v = v ushr 7
        }
        write(v)
    }
    fun writeVarLong(value: Long) {
        var v = value
        while (v >= 0x80) {
            write((v and 0x7F or 0x80).toInt())
            v = v ushr 7
        }
        write(v.toInt())
    }
}

val AudioInputStream.samplesCount get() = if (format.frameSize == -1 || frameLength == -1L) -1L
    else frameLength / (format.sampleSizeInBits / 8 * format.channels)

class RandomFileInputStream(file: File): InputStream() {
    private var markPos = 0L
    private val randomFile = RandomAccessFile(file, "r")
    val length get() = randomFile.length()
    @Throws(IOException::class)
    override fun read() = randomFile.read()
    @Synchronized
    override fun reset() { randomFile.seek(markPos) }
    @Throws(IOException::class)
    override fun close() { randomFile.close() }
    override fun markSupported() = true
    @Synchronized
    override fun mark(limit: Int) { markPos = randomFile.filePointer }
    @Throws(IOException::class)
    override fun skip(bytes: Long): Long {
        val pos = randomFile.filePointer
        randomFile.seek(pos + bytes)
        return randomFile.filePointer - pos
    }
    @Throws(IOException::class)
    override fun read(buffer: ByteArray) = randomFile.read(buffer)
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, pos: Int, bytes: Int) = randomFile.read(buffer, pos, bytes)
    @Throws(IOException::class)
    fun seek(pos: Long) { randomFile.seek(pos) }
}
