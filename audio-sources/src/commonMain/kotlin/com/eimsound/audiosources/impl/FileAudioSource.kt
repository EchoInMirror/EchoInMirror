package com.eimsound.audiosources.impl

import com.eimsound.audiosources.*
import javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import org.jflac.FLACDecoder
import org.jflac.sound.spi.FlacFileFormatType
import org.jflac.util.ByteData
import org.jflac.util.RingBuffer
import org.tritonus.share.sampled.FloatSampleTools
import java.io.*
import java.nio.file.Path
import javax.sound.sampled.*
import kotlin.math.ceil

private class JFlacRandomFileInputStream(file: File) : org.jflac.io.RandomFileInputStream(file) {
    private var markPos = 0L
    @Synchronized
    override fun reset() { randomFile.seek(markPos) }
    fun resetToStart() { randomFile.seek(0) }
    @Synchronized
    override fun mark(limit: Int) { markPos = randomFile.filePointer }
    @Throws(IOException::class)
    override fun seek(pos: Long) { randomFile.seek(pos) }
}

private val mpegFileReader by lazy { MpegAudioFileReader() }

class DefaultFileAudioSource(override val file: Path) : FileAudioSource {
    private var isWav = false
    private var isFlac = false

    override var sampleRate = 0F
        private set
    override var channels = 0
        private set
    override var length = 0L
        private set
    override val isClosed get() = stream == null
    override val isRandomAccessible get() = isWav || isFlac
    private var _position = 0L
    override var position: Long
        get() = _position
        set(value) {
            if (_position == value || !isRandomAccessible) return
            _position = value.coerceIn(0, length - 1)

            if (isFlac) {
                flacDecoder?.seek(_position)
                buffer.empty()
            }
        }

    private var frameSize = 0
    private lateinit var newFormat: AudioFormat

    private var stream: InputStream? = null
    private var flacDecoder: FLACDecoder? = null
    private var pcmData: ByteData? = null
    private var tempBuffer: ByteArray? = null
    private lateinit var buffer: RingBuffer

    init {
        run {
//            AudioSourceManager.instance.fileSourcesCache[file]?.get()?.let {
//                memoryAudioSource = it
//                sampleRate = it.sampleRate
//                channels = it.channels
//                length = it.length
//                return@run
//            }

            val randomStream = JFlacRandomFileInputStream(file.toFile())
            var format = AudioSystem.getAudioFileFormat(randomStream)
            if (format is MpegAudioFileFormat) {
                randomStream.close()
                format = mpegFileReader.getAudioFileFormat(file.toFile())
                length = ceil((format.properties()["duration"] as? Long ?: -1) / 1000000F * format.format.sampleRate).toLong()
            } else {
                randomStream.resetToStart()
                length = format.frameLength.toLong()
            }
            var bits = format.format.sampleSizeInBits
            if (bits == AudioSystem.NOT_SPECIFIED) bits = 16
            isWav = format.type == AudioFileFormat.Type.WAVE
            isFlac = format.type == FlacFileFormatType.FLAC
            sampleRate = format.format.sampleRate
            channels = format.format.channels
            if (channels < 0 || length < 0) throw UnsupportedOperationException("Unsupported file!")
            frameSize = channels * (bits / 8)
            newFormat = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bits,
                channels, frameSize, sampleRate, false)

            if (isWav) {
                stream = AudioSystem.getAudioInputStream(randomStream).apply { mark(0) }
            } else if (isFlac) {
                stream = randomStream
                buffer = RingBuffer()
                buffer.resize(frameSize * 2)
                flacDecoder = FLACDecoder(stream).apply { readMetadata() }
            } else {
                try {
                    stream = AudioSystem.getAudioInputStream(newFormat, AudioSystem.getAudioInputStream(file.toFile()))
                } catch (e: Exception) {
                    throw UnsupportedOperationException(e)
                }
            }

//            if (readAll) {
//                isFirstTimeToClose = true
//                memoryAudioSource = AudioSourceManager.instance.createMemorySource(this)
//                AudioSourceManager.instance.fileSourcesCache[file] = WeakReference(memoryAudioSource!!)
//            }
        }
    }

    override fun close() {
        stream?.close()
        stream = null
        if (::buffer.isInitialized) buffer.empty()
        pcmData = null
        tempBuffer = null
        flacDecoder = null
    }

    private fun readFlac(offset: Int, len: Int): Int {
        var offset2 = offset
        var len2 = len
        var bytesRead = 0
        // can only read integral number of frames
        len2 -= len2 % frameSize
        // do the best effort to fill the buffer
        val d = flacDecoder ?: return -1
        while (len2 > 0) {
            var thisLen = len2
            if (thisLen > buffer.available) thisLen = buffer.available
            if (thisLen < frameSize) {
                val frame = d.readNextFrame() ?: break
                val data = d.decodeFrame(frame, pcmData)
                pcmData = data
                buffer.resize(data.len * 2)
                buffer.put(data.data, 0, data.len)

                if (buffer.available < frameSize) break
                continue
            }
            // can only read integral number of frames
            thisLen -= (thisLen % frameSize)
            val thisBytesRead = buffer.get(tempBuffer, offset2, thisLen)
            if (thisBytesRead < frameSize) break
            offset2 += thisBytesRead
            len2 -= thisBytesRead
            bytesRead += thisBytesRead
        }
        return if (bytesRead < 1) -1 else bytesRead
    }

    override fun nextBlock(buffers: Array<FloatArray>, length: Int, offset: Int): Int {
        val sampleCount = length.coerceAtMost(buffers.firstOrNull()?.size ?: 0)
        // read into temporary byte buffer
        var byteBufferSize = sampleCount * frameSize
        var lTempBuffer = tempBuffer
        if (lTempBuffer == null || byteBufferSize > lTempBuffer.size) {
            lTempBuffer = ByteArray(byteBufferSize)
            tempBuffer = lTempBuffer
        }
        var readSamples = 0
        var byteOffset = 0
        while (readSamples < sampleCount) {
            val readBytes = if (isFlac) readFlac(byteOffset, byteBufferSize)
            else stream!!.read(lTempBuffer, byteOffset, byteBufferSize)
            if (readBytes < 0) {
                break
            } else if (readBytes == 0) {
                Thread.yield()
            } else {
                readSamples += readBytes / frameSize
                byteBufferSize -= readBytes
                byteOffset += readBytes
            }
        }
        // buffer.setSampleCount(offset + readSamples, offset > 0)
        if (readSamples > 0) {
            // convert
            FloatSampleTools.byte2float(
                lTempBuffer, 0, buffers, offset,
                readSamples, newFormat, false
            )
        }

        _position += readSamples
        return readSamples
    }

    override fun copy() = DefaultFileAudioSource(file)

    override fun toString(): String {
        return "DefaultFileAudioSource(file=$file, sampleRate=$sampleRate, channels=$channels, length=$length, isRandomAccessible=$isRandomAccessible)"
    }
}

class DefaultFileAudioSourceFactory : FileAudioSourceFactory {
    override val supportedFormats = listOf("wav", "flac", "mp3", "ogg", "aiff", "aif", "aifc", "au", "snd")
    override val name = "File"
    override fun createAudioSource(file: Path) = DefaultFileAudioSource(file)
}
