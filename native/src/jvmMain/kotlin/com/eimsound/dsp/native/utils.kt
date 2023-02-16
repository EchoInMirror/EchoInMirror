package com.eimsound.dsp.native

import java.io.File

fun getSampleBits(bits: Int) = (1 shl (8 * bits - 1)) - 1

fun isX86PEFile(file: File) = file.inputStream().use {
    it.skip(0x3c)
    val peOffset = it.read()
    it.skip((peOffset - 0x3d).toLong())
    it.read() == 0x50 && it.read() == 0x45
}
