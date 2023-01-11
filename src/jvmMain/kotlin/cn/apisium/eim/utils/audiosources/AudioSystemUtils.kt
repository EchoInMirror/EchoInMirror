package cn.apisium.eim.utils.audiosources

import cn.apisium.eim.utils.audiosources.flac.Flac2PcmAudioInputStream
import org.jflac.sound.spi.FlacEncoding
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

fun convertFormatToPcm(sourceStream: AudioInputStream): AudioInputStream {
    val format = sourceStream.format
    if (format.encoding == AudioFormat.Encoding.PCM_SIGNED) return sourceStream
    val newFormat = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, format.sampleSizeInBits,
        format.channels, format.channels * 2, format.sampleRate, false)
    if (sourceStream.format.encoding == FlacEncoding.FLAC) {
        return Flac2PcmAudioInputStream(sourceStream, newFormat, sourceStream.frameLength)
    }
    return AudioSystem.getAudioInputStream(newFormat, sourceStream)
}

fun AudioFormat.copy(
    sampleRate: Float = this.sampleRate,
    channels: Int = this.channels,
    sampleSizeInBits: Int = this.sampleSizeInBits,
    encoding: AudioFormat.Encoding = this.encoding,
    frameSize: Int = this.frameSize,
    frameRate: Float = this.frameRate,
    bigEndian: Boolean = this.isBigEndian
) = AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian)
