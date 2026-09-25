package com.tinyai.geekbox.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {

    private const val KIB = 1024.0
    private const val MIB = KIB * 1024
    private const val GIB = MIB * 1024
    private const val TIB = GIB * 1024

    fun bytes(value: Long, decimals: Int = 2): String {
        val v = value.toDouble()
        return when {
            abs(v) >= TIB -> "%.${decimals}f TiB".format(v / TIB)
            abs(v) >= GIB -> "%.${decimals}f GiB".format(v / GIB)
            abs(v) >= MIB -> "%.${decimals}f MiB".format(v / MIB)
            abs(v) >= KIB -> "%.${decimals}f KiB".format(v / KIB)
            else -> "$value B"
        }
    }

    fun hz(khz: Long): String = if (khz <= 0) "-" else "%.2f GHz".format(khz / 1_000_000.0)

    fun percent(part: Double, total: Double): Float =
        if (total <= 0) 0f else (part / total).coerceIn(0.0, 1.0).toFloat()

    fun percentText(part: Double, total: Double): String =
        if (total <= 0) "0%" else "%.1f%%".format(part / total * 100.0)

    fun uptime(millis: Long): String {
        val totalSeconds = millis / 1000
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            append("%02dh %02dm %02ds".format(hours, minutes, seconds))
        }
    }

    fun duration(millis: Long): String {
        val seconds = millis / 1000.0
        return if (seconds < 1) "${millis}ms" else "%.2fs".format(seconds)
    }

    fun time(millis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String =
        if (millis <= 0) "-" else SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

    fun roundTo(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (value * factor).roundToLong() / factor
    }
}
