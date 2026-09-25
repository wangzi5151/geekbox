package com.tinyai.geekbox.core.util

object JsonFormatter {

    fun pretty(input: String, indent: Int = 2): String {
        validateOrThrow(input)
        val pad = " ".repeat(indent.coerceIn(1, 8))
        val sb = StringBuilder(input.length + input.length / 2)
        var depth = 0
        var inString = false
        var escaped = false
        for (c in input) {
            if (inString) {
                sb.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                '{', '[' -> { sb.append(c); depth++; sb.append('\n'); repeat(depth) { sb.append(pad) } }
                '}', ']' -> { depth = (depth - 1).coerceAtLeast(0); sb.append('\n'); repeat(depth) { sb.append(pad) }; sb.append(c) }
                ',' -> { sb.append(c); sb.append('\n'); repeat(depth) { sb.append(pad) } }
                ':' -> sb.append(": ")
                ' ', '\t', '\n', '\r' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
            .replace(Regex("\\{\\s*\\}"), "{}")
            .replace(Regex("\\[\\s*\\]"), "[]")
    }

    fun minify(input: String): String {
        validateOrThrow(input)
        val sb = StringBuilder(input.length)
        var inString = false
        var escaped = false
        for (c in input) {
            if (inString) {
                sb.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                ' ', '\t', '\n', '\r' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun validate(input: String): String? = try {
        val parser = Parser(input)
        parser.skipWhitespace()
        if (parser.end) "输入为空" else {
            parser.parseValue()
            parser.skipWhitespace()
            if (!parser.end) "位置 ${parser.pos}: 存在多余内容" else null
        }
    } catch (e: JsonException) {
        e.message
    } catch (e: Exception) {
        e.message ?: "无效的 JSON"
    }

    fun validateOrThrow(input: String) {
        if (input.isBlank()) throw JsonException("输入为空")
        validate(input)?.let { throw JsonException(it) }
    }

    class JsonException(message: String) : IllegalArgumentException(message)

    private class Parser(private val text: String) {
        var pos = 0
        val end: Boolean get() = pos >= text.length

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun parseValue() {
            skipWhitespace()
            if (end) throw JsonException("位置 $pos: 期望值但已到结尾")
            when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> expect("true")
                'f' -> expect("false")
                'n' -> expect("null")
                else -> parseNumber()
            }
        }

        private fun parseObject() {
            pos++
            skipWhitespace()
            if (peek() == '}') { pos++; return }
            while (true) {
                skipWhitespace()
                if (peek() != '"') throw JsonException("位置 $pos: 对象键必须是字符串")
                parseString()
                skipWhitespace()
                if (peek() != ':') throw JsonException("位置 $pos: 期望 ':'")
                pos++
                parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return }
                    else -> throw JsonException("位置 $pos: 期望 ',' 或 '}'")
                }
            }
        }

        private fun parseArray() {
            pos++
            skipWhitespace()
            if (peek() == ']') { pos++; return }
            while (true) {
                parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return }
                    else -> throw JsonException("位置 $pos: 期望 ',' 或 ']'")
                }
            }
        }

        private fun parseString() {
            pos++
            while (pos < text.length) {
                when (text[pos]) {
                    '"' -> { pos++; return }
                    '\\' -> {
                        pos++
                        if (pos >= text.length) throw JsonException("位置 $pos: 字符串转义未结束")
                        when (text[pos]) {
                            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> pos++
                            'u' -> {
                                if (pos + 4 >= text.length) throw JsonException("位置 $pos: 非法 \\u 转义")
                                repeat(4) {
                                    val h = text[pos + 1 + it]
                                    if (h !in '0'..'9' && h !in 'a'..'f' && h !in 'A'..'F') {
                                        throw JsonException("位置 ${pos + 1}: 非法 \\u 十六进制")
                                    }
                                }
                                pos += 5
                            }
                            else -> throw JsonException("位置 $pos: 非法转义字符")
                        }
                    }
                    '\n', '\r' -> throw JsonException("位置 $pos: 字符串内不允许换行")
                    else -> pos++
                }
            }
            throw JsonException("位置 $pos: 字符串未闭合")
        }

        private fun parseNumber() {
            val start = pos
            if (peek() == '-') pos++
            if (peek() !in '0'..'9') throw JsonException("位置 $pos: 非法数值")
            while (peek() in '0'..'9') pos++
            if (peek() == '.') {
                pos++
                if (peek() !in '0'..'9') throw JsonException("位置 $pos: 小数点后应为数字")
                while (peek() in '0'..'9') pos++
            }
            if (peek() == 'e' || peek() == 'E') {
                pos++
                if (peek() == '+' || peek() == '-') pos++
                if (peek() !in '0'..'9') throw JsonException("位置 $pos: 指数部分应为数字")
                while (peek() in '0'..'9') pos++
            }
            if (pos == start) throw JsonException("位置 $pos: 期望值")
        }

        private fun expect(literal: String) {
            if (pos + literal.length > text.length || text.substring(pos, pos + literal.length) != literal) {
                throw JsonException("位置 $pos: 非法字面量")
            }
            pos += literal.length
        }

        private fun peek(): Char = if (pos < text.length) text[pos] else '\u0000'
    }
}
