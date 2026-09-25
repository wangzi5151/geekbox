package com.tinyai.geekbox.core.util

import kotlin.math.abs
import kotlin.math.roundToInt

object ColorUtils {

    data class Rgb(val r: Int, val g: Int, val b: Int)

    fun parseHex(input: String): Rgb? {
        val clean = input.trim().removePrefix("#").trim()
        return when (clean.length) {
            3 -> {
                val r = clean[0].digitToIntOrNull(16) ?: return null
                val g = clean[1].digitToIntOrNull(16) ?: return null
                val b = clean[2].digitToIntOrNull(16) ?: return null
                Rgb(r * 17, g * 17, b * 17)
            }
            6 -> {
                val r = clean.substring(0, 2).toIntOrNull(16) ?: return null
                val g = clean.substring(2, 4).toIntOrNull(16) ?: return null
                val b = clean.substring(4, 6).toIntOrNull(16) ?: return null
                Rgb(r, g, b)
            }
            8 -> {
                val r = clean.substring(2, 4).toIntOrNull(16) ?: return null
                val g = clean.substring(4, 6).toIntOrNull(16) ?: return null
                val b = clean.substring(6, 8).toIntOrNull(16) ?: return null
                Rgb(r, g, b)
            }
            else -> null
        }
    }

    fun toHex(rgb: Rgb): String = "#%02X%02X%02X".format(rgb.r, rgb.g, rgb.b)

    fun toHsl(rgb: Rgb): Triple<Float, Float, Float> {
        val r = rgb.r / 255f
        val g = rgb.g / 255f
        val b = rgb.b / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f else delta / (1 - abs(2 * l - 1))
        val h = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return Triple(if (h < 0) h + 360f else h, s * 100f, l * 100f)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Rgb {
        val sn = (s / 100f).coerceIn(0f, 1f)
        val ln = (l / 100f).coerceIn(0f, 1f)
        val c = (1 - abs(2 * ln - 1)) * sn
        val hh = ((h % 360f) + 360f) % 360f / 60f
        val x = c * (1 - abs(hh % 2 - 1))
        val (r1, g1, b1) = when {
            hh < 1 -> Triple(c, x, 0f)
            hh < 2 -> Triple(x, c, 0f)
            hh < 3 -> Triple(0f, c, x)
            hh < 4 -> Triple(0f, x, c)
            hh < 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = ln - c / 2
        return Rgb(
            ((r1 + m) * 255).roundToInt().coerceIn(0, 255),
            ((g1 + m) * 255).roundToInt().coerceIn(0, 255),
            ((b1 + m) * 255).roundToInt().coerceIn(0, 255)
        )
    }
}
