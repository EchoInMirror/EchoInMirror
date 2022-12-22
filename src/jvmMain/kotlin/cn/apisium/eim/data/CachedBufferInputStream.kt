package cn.apisium.eim.data

import java.io.InputStream

class CachedBufferInputStream(
    private val cacheSize: Int,
    private val length: Long,
    private val fn: (ByteArray) -> Unit
) : InputStream() {
    private val buffer = ByteArray(cacheSize)
    private var cursor = 0L

    override fun read() = if (cursor < length) {
        val pos = (cursor % cacheSize).toInt()
        if (pos == 0) {
            fn(buffer)
        }
        cursor++
        buffer[pos].toInt()
    } else -1

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (cursor >= length) return 0
        val pos = (cursor % cacheSize).toInt()
        if (pos == 0) fn(buffer)
        val size = len.coerceAtMost(cacheSize - pos)
        System.arraycopy(buffer, pos, b, off, size)
        cursor += size
        return size
    }

    override fun available() = (length - cursor).toInt()
}