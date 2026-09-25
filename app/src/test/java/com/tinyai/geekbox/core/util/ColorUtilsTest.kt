package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun parseShortHex() {
        assertEquals(ColorUtils.Rgb(255, 255, 255), ColorUtils.parseHex("#fff"))
        assertEquals(ColorUtils.Rgb(0, 0, 0), ColorUtils.parseHex("000"))
    }

    @Test
    fun parseFullHex() {
        assertEquals(ColorUtils.Rgb(34, 211, 238), ColorUtils.parseHex("#22D3EE"))
        assertEquals(ColorUtils.Rgb(18, 52, 86), ColorUtils.parseHex("#FF123456"))
    }

    @Test
    fun invalidHexReturnsNull() {
        assertNull(ColorUtils.parseHex("#12"))
        assertNull(ColorUtils.parseHex("zzzzzz"))
    }

    @Test
    fun toHexFormats() {
        assertEquals("#22D3EE", ColorUtils.toHex(ColorUtils.Rgb(34, 211, 238)))
    }

    @Test
    fun hslRoundTrip() {
        val rgb = ColorUtils.Rgb(255, 0, 0)
        val (h, s, l) = ColorUtils.toHsl(rgb)
        assertEquals(0f, h, 0.5f)
        assertEquals(100f, s, 0.5f)
        assertEquals(50f, l, 0.5f)
        val back = ColorUtils.fromHsl(h, s, l)
        assertEquals(rgb, back)
    }
}
