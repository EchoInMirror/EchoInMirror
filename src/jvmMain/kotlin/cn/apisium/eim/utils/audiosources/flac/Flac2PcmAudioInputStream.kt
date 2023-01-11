package cn.apisium.eim.utils.audiosources.flac

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import org.jflac.FLACDecoder
import org.jflac.PCMProcessor
import org.jflac.metadata.Metadata
import org.jflac.metadata.StreamInfo
import org.jflac.util.ByteData
import java.io.IOException
import java.io.InputStream
import javax.sound.sampled.AudioFormat

/**
 * Converts an Flac bitstream into a PCM 16bits/sample audio stream.
 *
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @author Florian Bomers
 * @version $Revision: 1.6 $
 */
class Flac2PcmAudioInputStream(input: InputStream, format: AudioFormat, length: Long) :
    RingedAudioInputStream(input, format, length), PCMProcessor {
    /** Flac Decoder.  */
    private var decoder: FLACDecoder = FLACDecoder(input).apply { addPCMProcessor(this@Flac2PcmAudioInputStream) }
    private var pcmData: ByteData? = null

    /** StreamInfo MetaData.  */
    @Suppress("unused")
    val streamInfo: StreamInfo get() = decoder.streamInfo

    /** the meta data from the stream  */
    @Suppress("unused")
    val metaData: Array<Metadata> = decoder.readMetadata()

    /**
     * called from the super class whenever more PCM data is needed.
     */
    @Throws(IOException::class)
    override fun fill() {
        if (decoder.isEOF) {
            buffer.isEOF = true
        } else {
            val frame = decoder.readNextFrame()
            if (frame != null) {
                pcmData = decoder.decodeFrame(frame, pcmData)
                processPCM(pcmData)
            }
        }
    }

    override fun processStreamInfo(streamInfo: StreamInfo) {
    }

    /**
     * Process the decoded PCM bytes. This is called synchronously from the
     * fill() method.
     *
     * @param pcm The decoded PCM data
     * @see org.jflac.PCMProcessor.processPCM
     */
    override fun processPCM(pcm: ByteData?) {
        buffer.resize(pcm!!.len * 2)
        buffer.put(pcm.data, 0, pcm.len)
    }
}
