package com.tinyai.geekbox.core.util

import java.security.SecureRandom

object RandomUtils {

    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"

    private val random = SecureRandom()

    fun password(
        length: Int,
        useUpper: Boolean = true,
        useLower: Boolean = true,
        useDigits: Boolean = true,
        useSymbols: Boolean = true
    ): String {
        val pool = buildString {
            if (useLower) append(LOWER)
            if (useUpper) append(UPPER)
            if (useDigits) append(DIGITS)
            if (useSymbols) append(SYMBOLS)
        }
        if (pool.isEmpty()) return ""
        return buildString {
            repeat(length.coerceAtLeast(1)) { append(pool[random.nextInt(pool.length)]) }
        }
    }

    fun pin(length: Int): String = buildString {
        repeat(length.coerceAtLeast(1)) { append(DIGITS[random.nextInt(DIGITS.length)]) }
    }

    fun hex(bytes: Int): String {
        val data = ByteArray(bytes.coerceAtLeast(1))
        random.nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }
}
