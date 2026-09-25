@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.StatBar
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.feature.system.AppUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ntpServers = listOf("ntp.aliyun.com", "cn.pool.ntp.org", "ntp.ntsc.ac.cn", "time.windows.com", "time.apple.com")

@Composable
fun NtpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var server by remember { mutableStateOf(ntpServers.first()) }
    var running by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<NtpResult>() }
    val scope = rememberCoroutineScope()

    ScreenScaffold("NTP 时间检查", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = server, onValueChange = { server = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("NTP 服务器") })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ntpServers.forEach { s ->
                    FilterChip(selected = server == s, onClick = { server = s }, label = { Text(s.substringBefore('.')) })
                }
            }
            Button(
                onClick = {
                    running = true
                    results.clear()
                    scope.launch {
                        results.add(withContext(Dispatchers.IO) { repo.ntpTime(server.trim()) })
                        running = false
                    }
                },
                enabled = !running && server.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "查询中…" else "校时")
            }
            results.forEach { r ->
                SectionCard("校时结果") {
                    if (r.error != null) {
                        Text("失败：${r.error}", color = MaterialTheme.colorScheme.error)
                    } else {
                        InfoRow("服务器时间", Formatters.time(r.serverMillis))
                        InfoRow("本机时间", Formatters.time(System.currentTimeMillis()))
                        InfoRow(
                            "偏差",
                            "${if (r.offsetMs >= 0) "+" else ""}${r.offsetMs} ms",
                            valueColor = if (kotlin.math.abs(r.offsetMs) > 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                        InfoRow("往返延迟", "${r.roundTripMs} ms")
                    }
                }
            }
            SectionCard {
                Text("通过 UDP 123 向 NTP 服务器取时并与本机对比。偏移为约值（未做完整 NTP 四时间戳校正）。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var mac by remember { mutableStateOf("") }
    var broadcast by remember { mutableStateOf("255.255.255.255") }
    var port by remember { mutableStateOf("9") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("Wake-on-LAN", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = mac, onValueChange = { mac = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("目标 MAC") }, placeholder = { Text("AA:BB:CC:DD:EE:FF") })
            OutlinedTextField(value = broadcast, onValueChange = { broadcast = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("广播地址") })
            OutlinedTextField(value = port, onValueChange = { port = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("端口") })
            Button(
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { repo.wakeOnLan(mac.trim(), broadcast.trim(), port.toIntOrNull() ?: 9) }
                        message = if (ok) "已发送魔术包" else "发送失败（检查 MAC 格式）"
                    }
                },
                enabled = mac.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("发送唤醒包")
            }
            message?.let { SectionCard { Text(it, color = MaterialTheme.colorScheme.onSurface) } }
            SectionCard {
                Text("向目标所在网段的广播地址发送标准 WOL 魔术包。需与目标处于同一局域网。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RedirectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var url by remember { mutableStateOf("http://github.com") }
    var running by remember { mutableStateOf(false) }
    val hops = remember { mutableStateListOf<RedirectHop>() }
    val scope = rememberCoroutineScope()

    ScreenScaffold("重定向追踪", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL") })
            Button(
                onClick = {
                    running = true
                    hops.clear()
                    scope.launch {
                        hops.addAll(withContext(Dispatchers.IO) { repo.redirectChain(url.trim()) })
                        running = false
                    }
                },
                enabled = !running && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "追踪中…" else "开始追踪") }
            SectionCard("跳转链 (${hops.size})") {
                if (hops.isEmpty()) Text("等待执行…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else hops.forEachIndexed { i, h ->
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Text("${i + 1}. [${h.status}]", style = MaterialTheme.typography.labelMedium, color = if (h.status in 300..399) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(h.url, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                        h.location?.let { Text("→ $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
fun PtrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var ip by remember { mutableStateOf("8.8.8.8") }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("反向解析 (PTR)", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = ip, onValueChange = { ip = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("IP 地址") })
            Button(
                onClick = {
                    running = true
                    result = null
                    val target = ip.trim()
                    scope.launch {
                        result = withContext(Dispatchers.IO) { repo.reverseLookup(target) }
                        running = false
                    }
                },
                enabled = !running && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "查询中…" else "反查主机名") }
            result?.let { SectionCard("结果") { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface) } }
        }
    }
}

@Composable
fun ListenPortsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    val usage = remember { AppUsageRepository(context) }
    var refresh by remember { mutableStateOf(0) }
    val state = rememberAsync(refresh) { repo.connections().filter { it.state == "LISTEN" || it.protocol.startsWith("UDP") } }

    ScreenScaffold("监听端口", onBack, actions = {
        IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    Text("共 ${s.value.size} 项（本机监听/绑定）。授权 Shizuku 可读取完整列表。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(s.value) { c ->
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${c.protocol}  ${c.local}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                                        Text(c.uid?.let { usage.labelForUid(it) } ?: "-", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
