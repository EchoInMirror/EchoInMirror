package cn.apisium.eim.utils

import org.junit.Test
import kotlin.test.assertEquals

internal class UtilsTest {
    @Test
    fun getSampleBitsTest() {
        assertEquals(127, getSampleBits(1))
        assertEquals(32767, getSampleBits(2))
        assertEquals(8388607, getSampleBits(3))
        assertEquals(2147483647, getSampleBits(4))
    }
}