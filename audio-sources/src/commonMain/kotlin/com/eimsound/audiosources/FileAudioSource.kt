package com.eimsound.audiosources

import com.eimsound.audioprocessor.*
import com.eimsound.daw.commons.json.asString
import javazoom.spi.mpeg.sampled.file.MpegAudioFileFormat
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import kotlinx.serialization.json.*
import org.jflac.FLACDecoder
import org.jflac.sound.spi.FlacFileFormatType
import org.jflac.util.ByteData
import org.jflac.util.RingBuffer
import org.tritonus.sampled.file.WaveAudioFileReader
import org.tritonus.share.sampled.FloatSampleTools
import java.io.*
import java.lang.ref.WeakReference
import java.nio.file.Path
import java.nio.file.Paths
import javax.sound.sampled.*
import kotlin.io.path.pathString
import kotlin.math.absoluteValue
import kotlin.math.ceil

private const val MB500 = 500 * 1024 * 1024

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

private val wavFileReader by lazy { WaveAudioFileReader() }
private val mpegFileReader by lazy { MpegAudioFileReader() }

class DefaultFileAudioSource(override val factory: FileAudioSourceFactory<*>, override val file: Path) : FileAudioSource {
    private var isWav = false
    private var isFlac = false

    override val source: Nothing? = null
    override var sampleRate = 0F
        private set
    override var channels = 0
        private set
    override var length = 0L
        private set
    override val isRandomAccessible get() = isWav || isFlac || memoryAudioSource != null

    private var frameSize = 0
    private lateinit var newFormat: AudioFormat

    private var stream: InputStream? = null
    private var flacDecoder: FLACDecoder? = null
    private var lastPos = -1L
    private var pcmData: ByteData? = null
    private var tempBuffer: ByteArray? = null
    private lateinit var buffer: RingBuffer
    private var memoryAudioSource: MemoryAudioSource? = null
    private var isFirstTimeToClose = false

    init {
        run {
            AudioSourceManager.instance.fileSourcesCache[file]?.get()?.let {
                memoryAudioSource = it
                sampleRate = it.sampleRate
                channels = it.channels
                length = it.length
                return@run
            }

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

            var readAll = AudioSourceManager.instance.cachedFileSize > 0 && length * frameSize < AudioSourceManager.instance.cachedFileSize

            if (isWav) {
                stream = wavFileReader.getAudioInputStream(randomStream).apply { mark(0) }
            } else if (isFlac) {
                stream = randomStream
                buffer = RingBuffer()
                buffer.resize(frameSize * 2)
                flacDecoder = FLACDecoder(stream).apply { readMetadata() }
            } else {
                if (length * frameSize > MB500) throw UnsupportedOperationException("File is large than 500mb!")
                readAll = true
                try {
                    stream = AudioSystem.getAudioInputStream(newFormat, AudioSystem.getAudioInputStream(file.toFile()))
                } catch (e: Exception) {
                    throw UnsupportedOperationException(e)
                }
            }

            if (readAll) {
                isFirstTimeToClose = true
                memoryAudioSource = AudioSourceManager.instance.createMemorySource(this)
                AudioSourceManager.instance.fileSourcesCache[file] = WeakReference(memoryAudioSource!!)
            }
        }
    }

    override fun getSamples(start: Long, length: Int, buffers: Array<FloatArray>): Int {
        memoryAudioSource?.let { return@getSamples it.getSamples(start, length, buffers) }

        val stream = stream ?: return 0
        val consumed: Int
        if (isFlac) {
            if (lastPos != -1L && (lastPos + length - start).absoluteValue > 4) {
                flacDecoder?.seek(start.coerceIn(0, this.length))
                buffer.empty()
            }
            consumed = readFromByteArray(buffers, length)
            lastPos = start
        } else {
            val len = this.length
            if (start > len) return 0
            if (lastPos != -1L && lastPos + length != start) {
                if (start > lastPos) {
                    stream.skip((start - lastPos) * frameSize)
                } else {
                    if (stream.markSupported()) stream.reset()
                    stream.skip(start * frameSize)
                }
            }
            consumed = readFromByteArray(buffers, length)
            lastPos = start
        }
        return consumed
    }

    override fun toJson() = buildJsonObject {
        put("factory", factory.name)
        put("file", file.pathString)
    }

    override fun fromJson(json: JsonElement) = throw UnsupportedOperationException()

    override fun close() {
        stream?.close()
        stream = null
        if (::buffer.isInitialized) buffer.empty()
        pcmData = null
        tempBuffer = null
        flacDecoder = null
        if (isFirstTimeToClose) isFirstTimeToClose = false
        else memoryAudioSource = null
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

    private fun readFromByteArray(buffers: Array<FloatArray>, sampleCount: Int): Int {
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
                lTempBuffer, 0, buffers, 0,
                readSamples, newFormat, false
            )
        }
        return readSamples
    }

    override fun copy() = DefaultFileAudioSource(factory, file)

    override fun toString(): String {
        return "DefaultFileAudioSource(file=$file, source=$source, sampleRate=$sampleRate, channels=$channels, length=$length, isRandomAccessible=$isRandomAccessible)"
    }
}

class DefaultFileAudioSourceFactory : FileAudioSourceFactory<DefaultFileAudioSource> {
    override val supportedFormats = listOf("wav", "flac", "mp3", "ogg", "aiff", "aif", "aifc", "au", "snd")
    override val name = "File"
    override fun createAudioSource(file: Path) = DefaultFileAudioSource(this, file)
    override fun createAudioSource(source: AudioSource?, json: JsonObject?): DefaultFileAudioSource {
        val file = json?.get("file")?.asString() ?: throw IllegalArgumentException("File not found!")
        return createAudioSource(Paths.get(file))
    }
}
