package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomUtilsTest {

    @Test
    fun passwordLengthAndCharset() {
        val pwd = RandomUtils.password(24)
        assertEquals(24, pwd.length)
        val digits = RandomUtils.password(20, useUpper = false, useLower = false, useSymbols = false)
        assertTrue(digits.all { it.isDigit() })
    }

    @Test
    fun pinIsDigits() {
        val pin = RandomUtils.pin(6)
        assertEquals(6, pin.length)
        assertTrue(pin.all { it.isDigit() })
    }

    @Test
    fun hexLength() {
        assertEquals(16, RandomUtils.hex(8).length)
    }
}
