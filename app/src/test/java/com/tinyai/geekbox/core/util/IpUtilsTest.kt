package com.tinyai.geekbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpUtilsTest {

    @Test
    fun parseAndFormat() {
        val value = IpUtils.parseIpv4("192.168.1.10")!!
        assertEquals("192.168.1.10", IpUtils.ipv4ToString(value))
        assertNull(IpUtils.parseIpv4("999.1.1.1"))
        assertNull(IpUtils.parseIpv4("192.168.1"))
    }

    @Test
    fun subnet24() {
        val info = IpUtils.subnet("192.168.1.10", 24)!!
        assertEquals("192.168.1.0", info.network)
        assertEquals("192.168.1.255", info.broadcast)
        assertEquals("192.168.1.1", info.firstHost)
        assertEquals("192.168.1.254", info.lastHost)
        assertEquals("255.255.255.0", info.netmask)
        assertEquals(254L, info.hostCount)
        assertTrue(info.isPrivate)
    }

    @Test
    fun subnet30() {
        val info = IpUtils.subnet("10.0.0.5", 30)!!
        assertEquals("10.0.0.4", info.network)
        assertEquals("10.0.0.7", info.broadcast)
        assertEquals("10.0.0.5", info.firstHost)
        assertEquals("10.0.0.6", info.lastHost)
        assertEquals(2L, info.hostCount)
    }

    @Test
    fun parseCidr() {
        val info = IpUtils.parseCidr("172.16.5.9/20")!!
        assertEquals("172.16.0.0", info.network)
        assertEquals(20, info.prefix)
        assertTrue(info.isPrivate)
    }
}
