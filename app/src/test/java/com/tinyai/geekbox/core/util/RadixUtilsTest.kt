package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadixUtilsTest {

    @Test
    fun hexToBinary() {
        assertEquals("11111111", RadixUtils.convert("FF", 16, 2))
    }

    @Test
    fun decimalToHex() {
        assertEquals("FF", RadixUtils.convert("255", 10, 16))
    }

    @Test
    fun binaryToDecimal() {
        assertEquals("10", RadixUtils.convert("1010", 2, 10))
    }

    @Test
    fun stripsPrefix() {
        assertEquals("15", RadixUtils.convert("0xF", 16, 10))
    }

    @Test
    fun invalid() {
        assertTrue(!RadixUtils.isValid("GG", 16))
        assertTrue(RadixUtils.isValid("FF", 16))
    }
}
