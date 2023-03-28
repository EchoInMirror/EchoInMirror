package com.eimsound.daw.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StreamsTest {
    @Test
    fun testVarInt() {
        arrayOf(1, 10, 127, 128, 126, 255, 256, 257, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000).forEach {
            val arrayStream = ByteArrayOutputStream(10)
            val output = ByteBufOutputStream(true, arrayStream)
            output.writeVarInt(it)
            output.flush()
            val result = ByteBufInputStream(true, ByteArrayInputStream(arrayStream.toByteArray())).readVarInt()
            assertEquals(it, result)
        }
    }
}