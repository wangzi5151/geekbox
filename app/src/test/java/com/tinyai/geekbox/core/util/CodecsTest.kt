package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecsTest {

    @Test
    fun base64RoundTrip() {
        val text = "GeekBox 极客工具箱"
        val encoded = Codecs.base64Encode(text)
        assertEquals(text, Codecs.base64Decode(encoded))
    }

    @Test
    fun hexRoundTrip() {
        val text = "Hello, GeekBox!"
        val hex = Codecs.hexEncode(text)
        assertEquals(text, Codecs.hexDecode(hex))
    }

    @Test
    fun hexEncodeUpperCase() {
        assertEquals("414243", Codecs.hexEncode("ABC", upper = true))
    }

    @Test
    fun knownDigests() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Codecs.digest("MD5", "abc"))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Codecs.digest("SHA-256", "abc")
        )
    }

    @Test
    fun base64UrlDecode() {
        assertEquals("{\"alg\":\"none\"}", Codecs.base64UrlDecodeToString("eyJhbGciOiJub25lIn0"))
    }

    @Test
    fun urlEncodeDecode() {
        val raw = "a b&c=d"
        val encoded = Codecs.urlEncode(raw)
        assertEquals(raw, Codecs.urlDecode(encoded))
    }

    @Test
    fun hashListHasFourEntries() {
        val hashes = Codecs.hashes("test")
        assertEquals(4, hashes.size)
        assertTrue(hashes.all { it.second.isNotBlank() })
    }
}
