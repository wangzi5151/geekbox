package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun prettyFormatsObject() {
        val input = """{"name":"geekbox","count":3,"nested":{"a":1,"b":[1,2,3]},"ok":true}"""
        val pretty = JsonFormatter.pretty(input)
        assert(pretty.contains("\n"))
        assert(pretty.contains("\"name\": \"geekbox\""))
        assert(pretty.contains("\"b\": ["))
    }

    @Test
    fun minifyRemovesWhitespace() {
        val input = "{\n  \"a\" : 1 ,\n  \"b\" : [ 1 , 2 ]\n}"
        assertEquals("{\"a\":1,\"b\":[1,2]}", JsonFormatter.minify(input))
    }

    @Test
    fun prettyKeepsStringSpaces() {
        val input = """{"msg":"hello   world"}"""
        val pretty = JsonFormatter.pretty(input)
        assert(pretty.contains("hello   world"))
    }

    @Test
    fun emptyContainersAreCompact() {
        assertEquals("{}", JsonFormatter.minify("{}"))
        assertEquals("[]", JsonFormatter.minify("[]"))
    }

    @Test
    fun validateAcceptsValid() {
        assertNull(JsonFormatter.validate("""{"a":[1,2,{"b":null}],"c":-1.5e10}"""))
    }

    @Test
    fun validateRejectsInvalid() {
        assertNotNull(JsonFormatter.validate("""{"a":}"""))
        assertNotNull(JsonFormatter.validate("""{a:1}"""))
        assertNotNull(JsonFormatter.validate("""[1,2,"""))
    }
}
