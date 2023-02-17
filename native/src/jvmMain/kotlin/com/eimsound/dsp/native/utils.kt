package com.eimsound.dsp.native

import java.io.File

fun getSampleBits(bits: Int) = (1 shl (8 * bits - 1)) - 1

fun File.isX86PEFile() = inputStream().use {
    try {
        if (it.read() != 0x4D || it.read() != 0x5A) return false
        it.skip(0x3AL)
        val peOffset = it.read() or (it.read() shl 8) or (it.read() shl 16) or (it.read() shl 24)
        it.skip(peOffset - 0x40L)
        it.read() == 0x50 && it.read() == 0x45 && it.read() == 0x00 && it.read() == 0x00 && it.read() == 0x4C && it.read() == 0x01
    } catch (e: Exception) { false }
}
