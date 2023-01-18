package com.eimsound.dsp.native

fun getSampleBits(bits: Int) = (1 shl (8 * bits - 1)) - 1

