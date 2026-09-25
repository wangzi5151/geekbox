package com.tinyai.geekbox.core.util

import java.util.Calendar

object CronUtils {

    data class Field(val values: BooleanArray, val restricted: Boolean) {
        operator fun contains(v: Int): Boolean = v in values.indices && values[v]
    }

    data class Cron(
        val minute: Field,
        val hour: Field,
        val dayOfMonth: Field,
        val month: Field,
        val dayOfWeek: Field
    )

    fun parse(expression: String): Cron {
        val parts = expression.trim().split(Regex("\\s+"))
        require(parts.size == 5) { "需要 5 个字段：分 时 日 月 周" }
        val dowRaw = parseField(parts[4], 0, 7)
        val dowValues = BooleanArray(7)
        for (i in 0..7) {
            if (i < dowRaw.values.size && dowRaw.values[i]) dowValues[i % 7] = true
        }
        return Cron(
            minute = parseField(parts[0], 0, 59),
            hour = parseField(parts[1], 0, 23),
            dayOfMonth = parseField(parts[2], 1, 31),
            month = parseField(parts[3], 1, 12),
            dayOfWeek = Field(dowValues, parts[4] != "*")
        )
    }

    private fun parseField(field: String, min: Int, max: Int): Field {
        val values = BooleanArray(max + 1)
        val restricted = field != "*"
        field.split(",").forEach { part ->
            val segment = part.trim()
            val stepParts = segment.split("/")
            val range = stepParts[0]
            val step = if (stepParts.size > 1) (stepParts[1].toIntOrNull() ?: 1) else 1
            val (start, end) = when {
                range == "*" -> min to max
                range.contains("-") -> {
                    val (a, b) = range.split("-", limit = 2)
                    (a.toIntOrNull() ?: min) to (b.toIntOrNull() ?: max)
                }
                else -> {
                    val v = range.toIntOrNull() ?: throw IllegalArgumentException("非法字段: $part")
                    v to v
                }
            }
            var i = start
            while (i <= end) {
                if (i in min..max) values[i] = true
                i += step.coerceAtLeast(1)
            }
        }
        return Field(values, restricted)
    }

    fun nextRuns(expression: String, count: Int, fromMillis: Long = System.currentTimeMillis()): List<Long> {
        val cron = parse(expression)
        val results = ArrayList<Long>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var guard = 0
        val maxIterations = 60 * 24 * 366 * 2
        while (results.size < count && guard < maxIterations) {
            guard++
            val minute = cal.get(Calendar.MINUTE)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val dom = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
            if (matches(cron, minute, hour, dom, month, dow)) {
                results.add(cal.timeInMillis)
            }
            cal.add(Calendar.MINUTE, 1)
        }
        return results
    }

    private fun matches(cron: Cron, minute: Int, hour: Int, dom: Int, month: Int, dow: Int): Boolean {
        if (minute !in cron.minute || hour !in cron.hour || month !in cron.month) return false
        val domMatch = dom in cron.dayOfMonth
        val dowMatch = dow in cron.dayOfWeek
        return if (cron.dayOfMonth.restricted && cron.dayOfWeek.restricted) {
            domMatch || dowMatch
        } else {
            domMatch && dowMatch
        }
    }
}
