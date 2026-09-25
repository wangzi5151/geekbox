package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DiffUtilsTest {

    @Test
    fun detectChanges() {
        val result = DiffUtils.diff("a\nb\nc", "a\nx\nc")
        val kinds = result.map { it.kind }
        assertEquals(listOf(DiffUtils.Kind.SAME, DiffUtils.Kind.REMOVE, DiffUtils.Kind.ADD, DiffUtils.Kind.SAME), kinds)
        assertEquals("b", result[1].text)
        assertEquals("x", result[2].text)
    }

    @Test
    fun identical() {
        val result = DiffUtils.diff("same\ntext", "same\ntext")
        assertEquals(2, result.size)
        assertEquals(true, result.all { it.kind == DiffUtils.Kind.SAME })
    }
}
