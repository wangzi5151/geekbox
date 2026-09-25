package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun bytesScales() {
        assertEquals("1.00 GiB", Formatters.bytes(1024L * 1024 * 1024))
        assertEquals("1.50 MiB", Formatters.bytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("512 B", Formatters.bytes(512))
    }

    @Test
    fun uptimeFormats() {
        val millis = (2L * 86_400 + 3 * 3600 + 4 * 60 + 5) * 1000
        assertEquals("2d 03h 04m 05s", Formatters.uptime(millis))
    }

    @Test
    fun percentComputes() {
        assertEquals(0.5f, Formatters.percent(50.0, 100.0), 0.0001f)
        assertEquals(0f, Formatters.percent(1.0, 0.0), 0.0001f)
    }
}
