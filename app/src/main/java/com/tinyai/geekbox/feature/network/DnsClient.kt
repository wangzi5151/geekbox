package com.tinyai.geekbox.feature.network

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsClient {

    data class DnsResult(
        val server: String,
        val host: String,
        val addresses: List<String>,
        val timeMs: Long,
        val error: String? = null
    )

    fun query(server: String, host: String, timeoutMs: Int = 2500): DnsResult {
        val start = System.nanoTime()
        return try {
            val query = buildQuery(host)
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val address = InetAddress.getByName(server)
                socket.send(DatagramPacket(query, query.size, address, 53))
                val buffer = ByteArray(1500)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                val elapsed = (System.nanoTime() - start) / 1_000_000
                DnsResult(server, host, parseAnswers(buffer, response.length), elapsed)
            }
        } catch (t: Throwable) {
            DnsResult(server, host, emptyList(), (System.nanoTime() - start) / 1_000_000, t.message ?: "查询失败")
        }
    }

    private fun buildQuery(host: String): ByteArray {
        val out = ByteArrayOutputStream()
        val id = (Math.random() * 65535).toInt()
        out.write(id shr 8)
        out.write(id and 0xFF)
        out.write(0x01)
        out.write(0x00)
        out.write(0)
        out.write(1)
        repeat(6) { out.write(0) }
        host.split(".").forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            if (bytes.size in 1..63) {
                out.write(bytes.size)
                out.write(bytes)
            }
        }
        out.write(0)
        out.write(0)
        out.write(1)
        out.write(0)
        out.write(1)
        return out.toByteArray()
    }

    private fun parseAnswers(data: ByteArray, length: Int): List<String> {
        val result = ArrayList<String>()
        val qdCount = u16(data, 4)
        val anCount = u16(data, 6)
        var pos = 12
        repeat(qdCount) {
            pos = skipName(data, pos)
            pos += 4
        }
        var i = 0
        while (i < anCount && pos + 10 <= length && pos >= 0) {
            pos = skipName(data, pos)
            if (pos + 10 > length) break
            val type = u16(data, pos)
            val clazz = u16(data, pos + 2)
            val rdLength = u16(data, pos + 8)
            pos += 10
            if (type == 1 && clazz == 1 && rdLength == 4 && pos + 4 <= length) {
                result.add(
                    "${data[pos].toInt() and 0xFF}.${data[pos + 1].toInt() and 0xFF}." +
                        "${data[pos + 2].toInt() and 0xFF}.${data[pos + 3].toInt() and 0xFF}"
                )
            }
            pos += rdLength
            i++
        }
        return result
    }

    private fun skipName(data: ByteArray, start: Int): Int {
        var pos = start
        while (pos < data.size) {
            val len = data[pos].toInt() and 0xFF
            if (len == 0) return pos + 1
            if ((len and 0xC0) == 0xC0) return pos + 2
            pos += 1 + len
        }
        return pos
    }

    private fun u16(data: ByteArray, pos: Int): Int {
        if (pos + 1 >= data.size) return 0
        return ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
    }
}
