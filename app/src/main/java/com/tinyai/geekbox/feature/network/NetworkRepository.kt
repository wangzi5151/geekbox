package com.tinyai.geekbox.feature.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.security.MessageDigest

data class NetAddress(val host: String, val prefix: Int, val isIpv6: Boolean)

data class NetInterface(
    val name: String,
    val up: Boolean,
    val addresses: List<NetAddress>
)

data class WifiSummary(
    val ssid: String?,
    val bssid: String?,
    val ip: String?,
    val linkSpeedMbps: Int,
    val rssi: Int,
    val frequencyMhz: Int
)

data class PingReply(val seq: Int?, val from: String?, val timeMs: String?, val ttl: String?, val note: String?)

data class PingSummary(val transmitted: Int, val received: Int, val lossPercent: Int, val avgMs: String?)

data class HttpResult(
    val status: Int,
    val message: String,
    val headers: List<Pair<String, String>>,
    val body: String,
    val timeMs: Long,
    val error: String?
)

data class WifiScanEntry(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String
)

data class NetConnection(
    val protocol: String,
    val local: String,
    val remote: String,
    val state: String,
    val uid: Int?
)

data class CertInfo(
    val host: String,
    val subject: String,
    val issuer: String,
    val serial: String,
    val notBefore: Long,
    val notAfter: Long,
    val sigAlg: String,
    val sha256: String,
    val sans: List<String>,
    val daysRemaining: Long,
    val valid: Boolean
)

data class NtpResult(
    val server: String,
    val serverMillis: Long,
    val offsetMs: Long,
    val roundTripMs: Long,
    val error: String? = null
)

data class RedirectHop(
    val url: String,
    val status: Int,
    val location: String?
)

class NetworkRepository(private val context: Context) {

    fun interfaces(): List<NetInterface> = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .map { nif ->
                val addresses = nif.interfaceAddresses.mapNotNull { ia ->
                    val addr = ia.address ?: return@mapNotNull null
                    NetAddress(addr.hostAddress ?: return@mapNotNull null, ia.networkPrefixLength.toInt(), addr is java.net.Inet6Address)
                }.sortedBy { it.isIpv6 }
                NetInterface(nif.name, nif.isUp, addresses)
            }
    } catch (_: Throwable) {
        emptyList()
    }

    fun wifi(): WifiSummary? = try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val info = wm?.connectionInfo
        if (info == null) null else {
            val ipInt = info.ipAddress
            val ip = if (ipInt == 0) null else "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
            WifiSummary(
                ssid = info.ssid?.takeIf { it != "<unknown ssid>" },
                bssid = info.bssid,
                ip = ip,
                linkSpeedMbps = info.linkSpeed,
                rssi = info.rssi,
                frequencyMhz = info.frequency
            )
        }
    } catch (_: Throwable) {
        null
    }

    fun networkCapabilities(): List<String> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return emptyList()
            val caps = cm.getNetworkCapabilities(network) ?: return emptyList()
            buildList {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WiFi")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("蜂窝网络")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("以太网")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) add("可访问互联网")
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) add("已验证")
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun scanWifi(): List<WifiScanEntry> {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return emptyList()
        @Suppress("DEPRECATION")
        runCatching { wm.startScan() }
        val results = try {
            wm.scanResults
        } catch (_: Throwable) {
            return emptyList()
        }
        return results.map { r ->
            WifiScanEntry(
                ssid = r.SSID?.takeIf { it.isNotBlank() } ?: "<隐藏>",
                bssid = r.BSSID ?: "-",
                rssi = r.level,
                frequency = r.frequency,
                capabilities = r.capabilities ?: ""
            )
        }.sortedByDescending { it.rssi }
    }

    fun trafficTotals(): Pair<Long, Long> =
        TrafficStats.getTotalRxBytes() to TrafficStats.getTotalTxBytes()

    fun connections(): List<NetConnection> {
        val files = listOf(
            "/proc/net/tcp" to "TCP",
            "/proc/net/tcp6" to "TCP6",
            "/proc/net/udp" to "UDP",
            "/proc/net/udp6" to "UDP6"
        )
        val useShizuku = ShizukuBridge.isAuthorized()
        val result = ArrayList<NetConnection>()
        files.forEach { (path, proto) ->
            val text = if (useShizuku) {
                ShizukuBridge.runShell("cat $path", 10).stdout
            } else {
                runCatching { java.io.File(path).readText() }.getOrDefault("")
            }
            result.addAll(parseProcNet(text, proto, path.contains("6")))
        }
        return result
    }

    private fun parseProcNet(text: String, proto: String, isV6: Boolean): List<NetConnection> =
        text.lineSequence().drop(1).mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 8) return@mapNotNull null
            val local = parseHexAddr(parts[1], isV6) ?: return@mapNotNull null
            val remote = parseHexAddr(parts[2], isV6) ?: return@mapNotNull null
            val state = if (proto.startsWith("UDP")) "UDP" else tcpState(parts[3])
            val uid = parts.getOrNull(7)?.toIntOrNull()
            NetConnection(proto, local, remote, state, uid)
        }.toList()

    private fun parseHexAddr(token: String, isV6: Boolean): String? {
        val parts = token.split(":")
        if (parts.size != 2) return null
        val port = parts[1].toIntOrNull(16) ?: return null
        val bytes = parts[0].chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }
        val addr = when {
            isV6 && bytes.size == 16 -> {
                val out = ByteArray(16)
                for (w in 0..3) for (b in 0..3) out[w * 4 + b] = bytes[w * 4 + (3 - b)]
                InetAddress.getByAddress(out)
            }
            bytes.size == 4 -> InetAddress.getByAddress(bytes.reversed().toByteArray())
            else -> return null
        }
        return "${addr.hostAddress}:$port"
    }

    private fun tcpState(hex: String): String = when (hex.uppercase()) {
        "01" -> "ESTABLISHED"
        "02" -> "SYN_SENT"
        "03" -> "SYN_RECV"
        "04" -> "FIN_WAIT1"
        "05" -> "FIN_WAIT2"
        "06" -> "TIME_WAIT"
        "07" -> "CLOSE"
        "08" -> "CLOSE_WAIT"
        "09" -> "LAST_ACK"
        "0A" -> "LISTEN"
        "0B" -> "CLOSING"
        else -> "STATE_$hex"
    }

    fun certificate(host: String, port: Int = 443): CertInfo? = try {
        val factory = javax.net.ssl.SSLContext.getDefault().socketFactory
        val socket = factory.createSocket() as javax.net.ssl.SSLSocket
        socket.connect(java.net.InetSocketAddress(host, port), 8000)
        socket.startHandshake()
        val cert = socket.session.peerCertificates.firstOrNull() as? java.security.cert.X509Certificate
        runCatching { socket.close() }
        if (cert == null) null else {
            val now = System.currentTimeMillis()
            val sans = runCatching {
                cert.subjectAlternativeNames?.mapNotNull { it.getOrNull(1)?.toString() } ?: emptyList()
            }.getOrDefault(emptyList())
            val sha256 = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                .joinToString(":") { "%02X".format(it) }
            CertInfo(
                host = host,
                subject = cert.subjectDN.name,
                issuer = cert.issuerDN.name,
                serial = cert.serialNumber.toString(16).uppercase(),
                notBefore = cert.notBefore.time,
                notAfter = cert.notAfter.time,
                sigAlg = cert.sigAlgName,
                sha256 = sha256,
                sans = sans,
                daysRemaining = (cert.notAfter.time - now) / 86_400_000,
                valid = now in cert.notBefore.time..cert.notAfter.time
            )
        }
    } catch (_: Throwable) {
        null
    }

    fun whois(rawTarget: String): String {
        val target = rawTarget.trim()
            .removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore(" ").trim()
        if (target.isBlank()) return "请输入域名或 IP"
        val server = if (target.matches(Regex("^(\\d{1,3}\\.){3}\\d{1,3}$"))) {
            "whois.arin.net"
        } else {
            tldWhois(target.substringAfterLast('.').lowercase())
        }
        return try {
            val first = queryWhois(server, target)
            val referral = Regex("(?im)^(refer|ReferralServer|whois):\\s*(?:whois://)?([^\\s]+)")
                .find(first)?.groupValues?.get(2)?.trim()
            if (!referral.isNullOrBlank() && !referral.equals(server, true)) {
                val second = runCatching { queryWhois(referral, target) }.getOrDefault("")
                if (second.isNotBlank()) second else first
            } else {
                first
            }
        } catch (t: Throwable) {
            "查询失败: ${t.message}"
        }
    }

    private fun tldWhois(tld: String): String = when (tld) {
        "com", "net" -> "whois.verisign-grs.com"
        "org" -> "whois.pir.org"
        "io" -> "whois.nic.io"
        "dev", "app" -> "whois.nic.google"
        "cn" -> "whois.cnnic.cn"
        "co" -> "whois.nic.co"
        "me" -> "whois.nic.me"
        "info" -> "whois.afilias.net"
        "xyz" -> "whois.nic.xyz"
        else -> "whois.iana.org"
    }

    private fun queryWhois(server: String, target: String): String {
        java.net.Socket().use { socket ->
            socket.connect(java.net.InetSocketAddress(server, 43), 8000)
            socket.soTimeout = 12000
            socket.getOutputStream().apply {
                write("$target\r\n".toByteArray())
                flush()
            }
            val bytes = socket.getInputStream().readBytes()
            return String(bytes, Charsets.UTF_8).take(20000)
        }
    }

    suspend fun download(url: String, target: File, onProgress: (Long, Long) -> Unit): Long =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()
            val total = connection.contentLengthLong
            target.parentFile?.mkdirs()
            var downloaded = 0L
            try {
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(16384)
                        while (true) {
                            if (!isActive) throw kotlinx.coroutines.CancellationException("已取消")
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            downloaded
        }

    suspend fun ntpTime(server: String): NtpResult = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val socket = DatagramSocket()
            socket.soTimeout = 4000
            val buffer = ByteArray(48)
            buffer[0] = 0x1B
            val address = InetAddress.getByName(server)
            socket.send(DatagramPacket(buffer, buffer.size, address, 123))
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val end = System.currentTimeMillis()
            socket.close()
            val seconds = ((buffer[40].toLong() and 0xFF) shl 24) or
                ((buffer[41].toLong() and 0xFF) shl 16) or
                ((buffer[42].toLong() and 0xFF) shl 8) or
                (buffer[43].toLong() and 0xFF)
            val serverMillis = (seconds - 2_208_988_800L) * 1000
            NtpResult(server, serverMillis, serverMillis - end, end - start)
        } catch (t: Throwable) {
            NtpResult(server, 0, 0, 0, t.message ?: "查询失败")
        }
    }

    fun wakeOnLan(mac: String, broadcast: String, port: Int): Boolean = try {
        val hex = mac.replace(Regex("[^0-9a-fA-F]"), "")
        require(hex.length == 12) { "MAC 需为 12 位十六进制" }
        val macBytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6)
        }
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(broadcast), port))
        }
        true
    } catch (_: Throwable) {
        false
    }

    suspend fun reverseLookup(ip: String): String = withContext(Dispatchers.IO) {
        runCatching { InetAddress.getByName(ip.trim()).canonicalHostName }.getOrDefault("-")
    }

    suspend fun redirectChain(url: String): List<RedirectHop> = withContext(Dispatchers.IO) {
        val hops = ArrayList<RedirectHop>()
        var current = url
        var guard = 0
        while (guard < 10) {
            guard++
            val connection = runCatching { URL(current).openConnection() as HttpURLConnection }.getOrNull() ?: break
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val status = runCatching { connection.responseCode }.getOrDefault(0)
            val location = connection.getHeaderField("Location")
            hops.add(RedirectHop(current, status, location))
            connection.disconnect()
            if (status in 300..399 && !location.isNullOrBlank()) {
                current = runCatching { URL(URL(current), location).toString() }.getOrDefault(location)
            } else {
                break
            }
        }
        hops
    }

    fun dnsServers(): List<String> = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val lp: LinkProperties? = cm.getLinkProperties(cm.activeNetwork)
        lp?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    suspend fun resolve(host: String): List<String> = withContext(Dispatchers.IO) {
        InetAddress.getAllByName(host).map { it.hostAddress ?: it.toString() }
    }

    suspend fun ping(
        host: String,
        count: Int,
        onLine: (String) -> Unit,
        onReply: (PingReply) -> Unit = {}
    ): PingSummary = withContext(Dispatchers.IO) {
        val command = arrayOf("sh", "-c", "ping -c $count -W 3 $host")
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var transmitted = 0
        var received = 0
        var loss = 0
        var avg: String? = null
        try {
            while (true) {
                if (!isActive) {
                    process.destroyForcibly()
                    break
                }
                val line = reader.readLine() ?: break
                onLine(line)
                val parsed = parsePingLine(line)
                if (parsed != null) {
                    onReply(parsed)
                    if (parsed.note == null) {
                        transmitted++
                        received++
                    }
                }
                val lossMatch = Regex("(\\d+)% packet loss").find(line)
                if (lossMatch != null) loss = lossMatch.groupValues[1].toInt()
                val avgMatch = Regex("= [\\d.]+/([\\d.]+)/").find(line)
                if (avgMatch != null) avg = avgMatch.groupValues[1] + " ms"
            }
        } finally {
            runCatching { reader.close() }
            runCatching { process.destroy() }
        }
        if (transmitted == 0 && loss > 0) {
            PingSummary(count, 0, loss, null)
        } else {
            PingSummary(transmitted.coerceAtLeast(count), received, loss, avg)
        }
    }

    private fun parsePingLine(line: String): PingReply? {
        if (!line.contains("icmp_seq")) {
            return null
        }
        val seq = Regex("icmp_seq=(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
        val from = Regex("from ([0-9a-fA-F:.]+)").find(line)?.groupValues?.get(1)
            ?: Regex("bytes from ([0-9a-fA-F:.]+)").find(line)?.groupValues?.get(1)
        val time = Regex("time=([\\d.]+ ms)").find(line)?.groupValues?.get(1)
        val ttl = Regex("ttl=(\\d+)").find(line)?.groupValues?.get(1)
        val note = when {
            line.contains("Time to live exceeded", ignoreCase = true) -> "TTL 超时"
            line.contains("unreachable", ignoreCase = true) -> "不可达"
            else -> null
        }
        return PingReply(seq, from, time, ttl, note)
    }

    suspend fun traceroute(
        host: String,
        maxHops: Int,
        onHop: (Int, String) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        for (ttl in 1..maxHops) {
            if (!isActive) break
            val command = arrayOf("sh", "-c", "ping -c 1 -W 2 -t $ttl $host")
            val process = ProcessBuilder(*command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val from = Regex("From ([0-9a-fA-F:.]+)").find(output)?.groupValues?.get(1)
                ?: Regex("bytes from ([0-9a-fA-F:.]+)").find(output)?.groupValues?.get(1)
            val time = Regex("time=([\\d.]+)").find(output)?.groupValues?.get(1)
            val label = when {
                from == null -> "* * *"
                time != null -> "$from  ${time} ms"
                else -> "$from  (TTL exceeded)"
            }
            onHop(ttl, label)
            if (time != null && from != null && isDestination(host, from)) break
        }
    }

    private fun isDestination(host: String, from: String): Boolean = try {
        InetAddress.getAllByName(host).any { it.hostAddress == from }
    } catch (_: Throwable) {
        host == from
    }

    suspend fun http(
        method: String,
        url: String,
        headers: List<Pair<String, String>>,
        body: String?
    ): HttpResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection)
            connection = conn
            conn.requestMethod = method.uppercase()
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = false
            headers.forEach { (k, v) -> if (k.isNotBlank()) conn.setRequestProperty(k, v) }
            if (body != null && method.uppercase() in listOf("POST", "PUT", "PATCH", "DELETE")) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val message = conn.responseMessage ?: ""
            val responseHeaders = conn.headerFields.entries
                .filter { it.key != null }
                .flatMap { (k, v) -> v.map { k to it } }
            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader()?.use { reader ->
                val sb = StringBuilder()
                val buffer = CharArray(4096)
                var total = 0
                var read: Int
                while (reader.read(buffer).also { read = it } >= 0) {
                    val chunk = String(buffer, 0, read)
                    if (total < 200_000) {
                        val allowed = (200_000 - total).coerceAtMost(chunk.length)
                        sb.append(chunk, 0, allowed)
                        total += allowed
                    }
                }
                sb.toString()
            } ?: ""
            HttpResult(status, message, responseHeaders, text, System.currentTimeMillis() - start, null)
        } catch (t: Throwable) {
            HttpResult(0, "", emptyList(), "", System.currentTimeMillis() - start, t.message ?: t.toString())
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun portScan(
        host: String,
        ports: List<Int>,
        timeoutMs: Int,
        onFound: (Int) -> Unit,
        onProgress: (Int, Int) -> Unit
    ): Unit = coroutineScope {
        val semaphore = Semaphore(64)
        val total = ports.size
        var done = 0
        ports.map { port ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(host, port), timeoutMs)
                            onFound(port)
                        }
                    } catch (_: Throwable) {
                    }
                    synchronized(this@NetworkRepository) {
                        done++
                        onProgress(done, total)
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun lanHosts(onFound: (String) -> Unit, onProgress: (Int) -> Unit): Unit = coroutineScope {
        val subnet = currentSubnet() ?: return@coroutineScope
        val semaphore = Semaphore(48)
        var done = 0
        (1..254).map { last ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val ip = "$subnet.$last"
                    if (isReachable(ip)) {
                        onFound(ip)
                    }
                    synchronized(this@NetworkRepository) {
                        done++
                        onProgress(done)
                    }
                }
            }
        }.awaitAll()
    }

    private fun isReachable(ip: String): Boolean = try {
        val process = ProcessBuilder("sh", "-c", "ping -c 1 -W 1 $ip").redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        text.contains("bytes from") || text.contains("icmp_seq")
    } catch (_: Throwable) {
        false
    }

    fun currentSubnet(): String? {
        val addr = interfaces().flatMap { it.addresses }
            .firstOrNull { !it.isIpv6 && it.host.startsWith("192.") || !it.isIpv6 && it.host.startsWith("10.") || !it.isIpv6 && it.host.startsWith("172.") }
            ?: interfaces().flatMap { it.addresses }.firstOrNull { !it.isIpv6 }
            ?: return null
        val parts = addr.host.split(".")
        if (parts.size != 4) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    fun currentIpv4(): String? = interfaces().flatMap { it.addresses }
        .firstOrNull { !it.isIpv6 && it.host != "127.0.0.1" }?.host
}
