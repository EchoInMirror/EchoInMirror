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

import org.jflac.util.RingBuffer
import java.io.IOException
import java.io.InputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream

abstract class RingedAudioInputStream(
    /** The underlying inputStream.  */
    protected var `in`: InputStream?,
    format: AudioFormat,
    length: Long
) : AudioInputStream(`in`, format, length) {
    private val single = ByteArray(1)

    protected var buffer = RingBuffer()

    @Throws(IOException::class)
    private fun checkIfStillOpen() {
        if (`in` == null) throw IOException("Stream closed")
    }

    init {
        if (format.frameSize > 0) buffer.resize(format.frameSize * 2)
    }

    /**
     * Fills the buffer with more data, taking into account shuffling and other
     * tricks for dealing with marks. Assumes that it is being called by a
     * synchronized method. This method also assumes that all data has already
     * been read in, hence pos > count.
     *
     * @exception IOException
     */
    @Throws(IOException::class)
    protected abstract fun fill()
    /**
     *
     */
    /*
    protected void makeSpace() {
        if (markpos < 0)
            pos = 0; // no mark: throw away the buffer
        else if (pos >= buf.length) // no room left in buffer
        if (markpos > 0) { // can throw away early part of the buffer
            int sz = pos - markpos;
            System.arraycopy(buf, markpos, buf, 0, sz);
            pos = sz;
            markpos = 0;
        } else if (buf.length >= marklimit) {
            markpos = -1; // buffer got too big, invalidate mark
            pos = 0; // drop buffer contents
        } else { // grow buffer
            int nsz = pos * 2;
            if (nsz > marklimit) nsz = marklimit;
            byte[] nbuf = new byte[nsz];
            System.arraycopy(buf, 0, nbuf, 0, pos);
            buf = nbuf;
        }
        count = pos;
    }
*/
    /**
     * See the general contract of the `read` method of
     * `InputStream`.
     *
     * @return the next byte of data, or `-1` if the end of the
     * stream is reached.
     * @exception IOException
     * if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun read(): Int {
        fill()
        return if (buffer[single, 0, 1] == -1) {
            -1
        } else {
            single[0].toInt() and 0xFF
        }
    }
    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary.
     */
    //private int read1(byte[] b, int off, int len) throws IOException {
    //    return buffer.get(b, off, len);
    //}
    /**
     * Reads bytes from this byte-input stream into the specified byte array,
     * starting at the given offset.
     *
     *
     *
     * This method implements the general contract of the corresponding
     * `[read][InputStream.read]` method of
     * the `[InputStream]` class. As an additional
     * convenience, it attempts to read as many bytes as possible by repeatedly
     * invoking the `read` method of the underlying stream. This
     * iterated `read` continues until one of the following
     * conditions becomes true:
     *
     *
     *  * The specified number of bytes have been read,
     *
     *  * The `read` method of the underlying stream returns
     * `-1`, indicating end-of-file, or
     *
     *  * The `available` method of the underlying stream returns
     * zero, indicating that further input requests would block.
     *
     *
     * If the first `read` on the underlying stream returns
     * `-1` to indicate end-of-file then this method returns
     * `-1`. Otherwise this method returns the number of bytes
     * actually read.
     *
     *
     *
     * Subclasses of this class are encouraged, but not required, to attempt to
     * read as many bytes as possible in the same fashion.
     *
     * @param b
     * destination buffer.
     * @param off
     * offset at which to start storing bytes.
     * @param len
     * maximum number of bytes to read.
     * @return the number of bytes read, or `-1` if the end of the
     * stream has been reached.
     * @exception IOException
     * if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var offset = off
        var len2 = len
        checkIfStillOpen()
        val frameSize = format.frameSize
        var bytesRead = 0
        // can only read integral number of frames
        len2 -= len2 % frameSize
        // do a best effort to fill the buffer
        while (len2 > 0) {
            var thisLen = len2
            if (thisLen > buffer.available) {
                thisLen = buffer.available
            }
            if (thisLen < frameSize) {
                fill()
                if (buffer.available < frameSize) {
                    break
                }
                continue
            }
            // can only read integral number of frames
            thisLen -= (thisLen % frameSize)
            val thisBytesRead = buffer[b, offset, thisLen]
            if (thisBytesRead < frameSize) {
                break
            }
            offset += thisBytesRead
            len2 -= thisBytesRead
            bytesRead += thisBytesRead
        }
        return if (bytesRead == 0 && buffer.isEOF) {
            -1
        } else bytesRead
    }

    /**
     * See the general contract of the `skip` method of
     * `InputStream`.
     *
     * @param n
     * the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @exception IOException
     * if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        checkIfStillOpen()
        throw IOException("skip not supported")
    }

    /**
     * Returns the number of bytes that can be read from this inputstream
     * without blocking.
     *
     *
     * The `available` method of
     * `FilteredAudioInputStream` returns the sum of the the number
     * of bytes remaining to be read in the buffer (`count - pos`).
     * The result of calling the `available` method of the
     * underlying inputstream is not used, as this data will have to be
     * filtered, and thus may not be the same size after processing (although
     * subclasses that do the filtering should override this method and use the
     * amount of data available in the underlying inputstream).
     *
     * @return the number of bytes that can be read from this inputstream
     * without blocking.
     * @exception IOException
     * if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun available(): Int {
        checkIfStillOpen()
        if (buffer.available < getFrameLength().toInt()) {
            fill()
        }
        return buffer.available
    }

    /**
     * See the general contract of the `mark` method of
     * `InputStream`.
     *
     * @param readlimit
     * the maximum limit of bytes that can be read before the mark
     * position becomes invalid.
     * @see .reset
     */
    @Synchronized
    override fun mark(readlimit: Int) {
    }

    /**
     * See the general contract of the `reset` method of
     * `InputStream`.
     *
     *
     * If `markpos` is -1 (no mark has been set or the mark has
     * been invalidated), an `IOException` is thrown. Otherwise,
     * `pos` is set equal to `markpos`.
     *
     * @exception IOException
     * if this stream has not been marked or if the mark has been
     * invalidated.
     * @see .mark
     */
    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        checkIfStillOpen()
        throw IOException("reset not supported")
    }

    /**
     * Tests if this input stream supports the `mark` and
     * `reset` methods. The `markSupported` method of
     * `FilteredAudioInputStream` returns `true`.
     *
     * @return  a `boolean` indicating if this stream type supports
     * the `mark` and `reset` methods.
     * @see .mark
     * @see .reset
     */
    override fun markSupported(): Boolean {
        return false
    }

    /**
     * Closes this input stream and releases any system resources associated with
     * the stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        if (`in` == null) return
        `in`!!.close()
        `in` = null
    }
}
