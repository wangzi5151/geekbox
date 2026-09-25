package com.tinyai.geekbox.core.util

import java.math.BigInteger

object RadixUtils {

    fun convert(value: String, fromRadix: Int, toRadix: Int): String {
        val cleaned = clean(value)
        require(cleaned.isNotEmpty()) { "输入为空" }
        val negative = cleaned.startsWith("-")
        val digits = if (negative) cleaned.substring(1) else cleaned
        require(digits.isNotEmpty()) { "缺少数字" }
        val number = BigInteger(digits, fromRadix.coerceIn(2, 36))
        val text = number.toString(toRadix.coerceIn(2, 36)).uppercase()
        return if (negative) "-$text" else text
    }

    private fun clean(value: String): String {
        var v = value.trim().replace("_", "").replace(" ", "")
        val lower = v.lowercase()
        if (lower.startsWith("0x")) v = v.substring(2)
        else if (lower.startsWith("0b")) v = v.substring(2)
        else if (lower.startsWith("0o")) v = v.substring(2)
        return v
    }

    fun isValid(value: String, radix: Int): Boolean = try {
        convert(value, radix, 10)
        true
    } catch (_: Throwable) {
        false
    }
}
