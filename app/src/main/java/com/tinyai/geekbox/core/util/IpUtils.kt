package com.tinyai.geekbox.core.util

object IpUtils {

    data class SubnetInfo(
        val input: String,
        val prefix: Int,
        val network: String,
        val broadcast: String,
        val firstHost: String,
        val lastHost: String,
        val netmask: String,
        val wildcard: String,
        val hostCount: Long,
        val isPrivate: Boolean
    )

    fun parseIpv4(ip: String): Long? {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return null
        var value = 0L
        for (p in parts) {
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            value = (value shl 8) or n.toLong()
        }
        return value
    }

    fun ipv4ToString(value: Long): String =
        "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"

    fun subnet(ip: String, prefix: Int): SubnetInfo? {
        val value = parseIpv4(ip) ?: return null
        val p = prefix.coerceIn(0, 32)
        val mask = if (p == 0) 0L else (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        val network = value and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val hostBits = 32 - p
        val total = if (hostBits >= 32) 0x100000000L else 1L shl hostBits
        val hosts = when {
            hostBits == 0 -> 1L
            hostBits == 1 -> 0L
            else -> total - 2
        }
        val first = if (hostBits <= 1) network else network + 1
        val last = if (hostBits <= 1) broadcast else broadcast - 1
        val firstOctet = ((value shr 24) and 0xFF).toInt()
        val secondOctet = ((value shr 16) and 0xFF).toInt()
        val isPrivate = firstOctet == 10 ||
            (firstOctet == 172 && secondOctet in 16..31) ||
            (firstOctet == 192 && secondOctet == 168)
        return SubnetInfo(
            input = ipv4ToString(value),
            prefix = p,
            network = ipv4ToString(network),
            broadcast = ipv4ToString(broadcast),
            firstHost = ipv4ToString(first),
            lastHost = ipv4ToString(last),
            netmask = ipv4ToString(mask),
            wildcard = ipv4ToString(mask.inv() and 0xFFFFFFFFL),
            hostCount = hosts,
            isPrivate = isPrivate
        )
    }

    fun parseCidr(input: String): SubnetInfo? {
        val text = input.trim()
        return if (text.contains("/")) {
            val (ip, prefixText) = text.split("/", limit = 2)
            val prefix = prefixText.toIntOrNull() ?: return null
            subnet(ip, prefix)
        } else {
            parseIpv4(text)?.let { subnet(text, 24) }
        }
    }
}
