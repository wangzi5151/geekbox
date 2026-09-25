package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CronUtilsTest {

    @Test
    fun everyFifteenMinutes() {
        val runs = CronUtils.nextRuns("*/15 * * * *", 4, System.currentTimeMillis())
        assertEquals(4, runs.size)
        runs.forEach {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            assertEquals(0, cal.get(Calendar.MINUTE) % 15)
            assertEquals(0, cal.get(Calendar.SECOND))
        }
    }

    @Test
    fun dailyAtMidnight() {
        val runs = CronUtils.nextRuns("0 0 * * *", 1, System.currentTimeMillis())
        assertEquals(1, runs.size)
        val cal = Calendar.getInstance().apply { timeInMillis = runs.first() }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun invalidThrows() {
        var thrown = false
        try {
            CronUtils.parse("* * *")
        } catch (_: Throwable) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
