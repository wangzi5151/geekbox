@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun NetworkInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    val state = rememberAsync { repo.interfaces() }
    val wifi = remember { runCatching { repo.wifi() }.getOrNull() }
    val caps = remember { runCatching { repo.networkCapabilities() }.getOrDefault(emptyList()) }
    val dns = remember { runCatching { repo.dnsServers() }.getOrDefault(emptyList()) }

    ScreenScaffold("网络信息", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("连接") {
                if (caps.isEmpty()) {
                    Text("无活动网络", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    caps.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
                }
            }
            if (wifi != null) {
                SectionCard("WiFi") {
                    InfoRow("SSID", wifi.ssid ?: "未知(需定位权限)", mono = true)
                    InfoRow("BSSID", wifi.bssid ?: "-", mono = true)
                    InfoRow("IP", wifi.ip ?: "-", mono = true)
                    InfoRow("信号", "${wifi.rssi} dBm")
                    InfoRow("链路速度", "${wifi.linkSpeedMbps} Mbps")
                    InfoRow("频率", "${wifi.frequencyMhz} MHz")
                }
            }
            SectionCard("DNS 服务器") {
                if (dns.isEmpty()) Text("不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else dns.forEach { InfoRow("服务器", it, mono = true) }
            }
            AsyncContent(state) { list ->
                list.forEach { nif ->
                    SectionCard(nif.name) {
                        nif.addresses.forEach { addr ->
                            InfoRow(if (addr.isIpv6) "IPv6" else "IPv4", "${addr.host}/${addr.prefix}", mono = true)
                        }
                        if (nif.addresses.isEmpty()) {
                            Text("无地址", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DnsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var host by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("DNS 查询", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("域名或主机") },
                placeholder = { Text("example.com") }
            )
            Button(
                onClick = {
                    if (host.isBlank()) return@Button
                    results.clear()
                    error = null
                    loading = true
                    scope.launch {
                        runCatching { repo.resolve(host.trim()) }
                            .onSuccess { results.addAll(it) }
                            .onFailure { error = it.message }
                        loading = false
                    }
                },
                enabled = !loading && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "查询中…" else "查询") }
            SectionCard("解析结果 (${results.size})") {
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    results.isEmpty() -> Text("等待查询…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> results.forEach { CodeBlock(it) }
                }
            }
        }
    }
}

@Composable
fun PingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var host by remember { mutableStateOf("8.8.8.8") }
    var count by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<String>() }
    var summary by remember { mutableStateOf<PingSummary?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("Ping", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("主机") },
                placeholder = { Text("8.8.8.8 或 example.com") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16).forEach { n ->
                    FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n 次") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        lines.clear()
                        summary = null
                        running = true
                        job = scope.launch {
                            runCatching {
                                repo.ping(host.trim(), count, onLine = { lines.add(it) })
                            }.onSuccess { summary = it }
                                .onFailure { lines.add("错误: ${it.message}") }
                            running = false
                        }
                    },
                    enabled = !running && host.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("开始")
                }
                OutlinedButton(onClick = { job?.cancel(); running = false }, enabled = running, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("停止")
                }
            }
            summary?.let { s ->
                SectionCard("统计") {
                    InfoRow("发送", s.transmitted.toString())
                    InfoRow("接收", s.received.toString())
                    InfoRow("丢包率", "${s.lossPercent}%", valueColor = if (s.lossPercent > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    s.avgMs?.let { InfoRow("平均延迟", it) }
                }
            }
            SectionCard("输出") {
                if (lines.isEmpty()) Text("等待执行…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Text(
                    lines.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TracerouteScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var host by remember { mutableStateOf("8.8.8.8") }
    var maxHops by remember { mutableIntStateOf(30) }
    var running by remember { mutableStateOf(false) }
    val hops = remember { mutableStateListOf<Pair<Int, String>>() }
    val scope = rememberCoroutineScope()

    ScreenScaffold("Traceroute", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("目标主机") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30).forEach { n ->
                    FilterChip(selected = maxHops == n, onClick = { maxHops = n }, label = { Text("$n 跳") })
                }
            }
            Button(
                onClick = {
                    hops.clear()
                    running = true
                    scope.launch {
                        runCatching {
                            repo.traceroute(host.trim(), maxHops) { ttl, label -> hops.add(ttl to label) }
                        }.onFailure { hops.add(0 to "错误: ${it.message}") }
                        running = false
                    }
                },
                enabled = !running && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "追踪中…" else "开始追踪") }
            SectionCard("路径 (${hops.size})") {
                if (hops.isEmpty()) Text("等待执行…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else hops.forEach { (ttl, label) ->
                    InfoRow("$ttl", label, mono = true)
                }
            }
        }
    }
}
