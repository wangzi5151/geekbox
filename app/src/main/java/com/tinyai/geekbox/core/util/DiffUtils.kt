package com.tinyai.geekbox.core.util

object DiffUtils {

    enum class Kind { SAME, ADD, REMOVE }

    data class Line(val kind: Kind, val text: String)

    fun diff(a: String, b: String, maxLines: Int = 1000): List<Line> {
        val aLines = a.split("\n")
        val bLines = b.split("\n")
        if (aLines.size > maxLines || bLines.size > maxLines) {
            return fallback(aLines, bLines)
        }
        val n = aLines.size
        val m = bLines.size
        val width = m + 1
        val dp = IntArray((n + 1) * width)
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i * width + j] = if (aLines[i] == bLines[j]) {
                    dp[(i + 1) * width + (j + 1)] + 1
                } else {
                    maxOf(dp[(i + 1) * width + j], dp[i * width + (j + 1)])
                }
            }
        }
        val result = ArrayList<Line>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                aLines[i] == bLines[j] -> { result.add(Line(Kind.SAME, aLines[i])); i++; j++ }
                dp[(i + 1) * width + j] >= dp[i * width + (j + 1)] -> { result.add(Line(Kind.REMOVE, aLines[i])); i++ }
                else -> { result.add(Line(Kind.ADD, bLines[j])); j++ }
            }
        }
        while (i < n) { result.add(Line(Kind.REMOVE, aLines[i])); i++ }
        while (j < m) { result.add(Line(Kind.ADD, bLines[j])); j++ }
        return result
    }

    private fun fallback(aLines: List<String>, bLines: List<String>): List<Line> {
        val result = ArrayList<Line>()
        val max = maxOf(aLines.size, bLines.size)
        for (i in 0 until max) {
            val x = aLines.getOrNull(i)
            val y = bLines.getOrNull(i)
            when {
                x == y && x != null -> result.add(Line(Kind.SAME, x))
                else -> {
                    if (x != null) result.add(Line(Kind.REMOVE, x))
                    if (y != null) result.add(Line(Kind.ADD, y))
                }
            }
        }
        return result
    }
}
