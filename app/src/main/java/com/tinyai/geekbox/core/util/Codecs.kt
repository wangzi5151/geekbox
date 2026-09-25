package com.tinyai.geekbox.core.util

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

object Codecs {

    fun base64Encode(text: String, urlSafe: Boolean = false, noWrap: Boolean = false): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val encoder = if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
        val finalEncoder = if (noWrap) encoder.withoutPadding() else encoder
        return finalEncoder.encodeToString(bytes)
    }

    fun base64Decode(text: String): String {
        val input = text.trim().replace("\n", "").replace("\r", "").replace(" ", "")
        val decoder = if (input.contains('-') || input.contains('_')) Base64.getUrlDecoder() else Base64.getMimeDecoder()
        return String(decoder.decode(input), Charsets.UTF_8)
    }

    fun hexEncode(text: String, upper: Boolean = false, spaced: Boolean = false): String {
        val hex = text.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
        val result = if (upper) hex.uppercase() else hex
        return if (spaced) result.chunked(2).joinToString(" ") else result
    }

    fun hexDecode(text: String): String {
        val clean = text.replace(Regex("[^0-9a-fA-F]"), "")
        require(clean.length % 2 == 0) { "十六进制长度必须为偶数" }
        val bytes = ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun digest(algorithm: String, text: String): String =
        digestBytes(algorithm, text.toByteArray(Charsets.UTF_8))

    fun digestBytes(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }

    fun hashes(text: String): List<Pair<String, String>> = listOf(
        "MD5" to digest("MD5", text),
        "SHA-1" to digest("SHA-1", text),
        "SHA-256" to digest("SHA-256", text),
        "SHA-512" to digest("SHA-512", text)
    )

    fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8")

    fun urlDecode(text: String): String = URLDecoder.decode(text, "UTF-8")

    fun base64UrlDecodeToString(text: String): String {
        var t = text.trim().replace('-', '+').replace('_', '/')
        while (t.length % 4 != 0) t += "="
        return String(Base64.getDecoder().decode(t), Charsets.UTF_8)
    }

    fun uuid(): String = UUID.randomUUID().toString()

    fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    fun epochToText(epochSeconds: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String =
        Formatters.time(epochSeconds * 1000, pattern)

    fun textToEpoch(text: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): Long =
        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).parse(text.trim())?.time?.div(1000)
            ?: error("无法解析时间")
}
