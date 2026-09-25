@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.network

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.feature.system.AppUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppTrafficScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val usageRepo = remember { AppUsageRepository(context) }
    var hasAccess by remember { mutableStateOf(usageRepo.hasUsageAccess()) }
    var periodHours by remember { mutableStateOf(24) }
    var refresh by remember { mutableStateOf(0) }
    val state = rememberAsync(hasAccess, periodHours, refresh) {
        if (hasAccess) usageRepo.trafficByUid(System.currentTimeMillis() - periodHours * 3_600_000L) else emptyList()
    }

    ScreenScaffold("应用流量", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!hasAccess) {
                SectionCard("需要「使用情况访问」权限") {
                    Text(
                        "统计各应用流量需要授予「使用情况访问」。GeekBox 仅本机读取，不会上传。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("打开设置") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { hasAccess = usageRepo.hasUsageAccess() }, modifier = Modifier.fillMaxWidth()) {
                        Text("我已授权，重新检测")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 小时", 24 to "24 小时", 168 to "7 天").forEach { (h, label) ->
                        FilterChip(selected = periodHours == h, onClick = { periodHours = h }, label = { Text(label) })
                    }
                }
                when (val s = state) {
                    is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                    is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                    is Async.Success -> {
                        val total = s.value.sumOf { it.total }
                        SectionCard("合计") {
                            InfoRow("总流量", Formatters.bytes(total))
                            InfoRow("应用数", s.value.size.toString())
                        }
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(s.value) { t ->
                                androidx.compose.material3.Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(usageRepo.labelForUid(t.uid), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                            Text(Formatters.bytes(t.total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "↓ ${Formatters.bytes(t.rx)}   ↑ ${Formatters.bytes(t.tx)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

@Composable
fun CertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var host by remember { mutableStateOf("github.com") }
    var portText by remember { mutableStateOf("443") }
    var runKey by remember { mutableStateOf(0) }
    val state = rememberAsync(runKey) {
        if (runKey == 0) null else repo.certificate(host.trim(), portText.toIntOrNull() ?: 443)
    }

    ScreenScaffold("TLS 证书检查", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("主机") })
            OutlinedTextField(value = portText, onValueChange = { portText = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("端口") })
            Button(onClick = { runKey++ }, enabled = host.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("获取证书")
            }
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    val c = s.value
                    if (c == null) {
                        SectionCard { Text("无法获取证书（连接失败或非 TLS）。", color = MaterialTheme.colorScheme.error) }
                    } else {
                        SectionCard("有效性") {
                            InfoRow("状态", if (c.valid) "有效" else "无效/过期", valueColor = if (c.valid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                            InfoRow("剩余天数", c.daysRemaining.toString(), valueColor = if (c.daysRemaining < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            InfoRow("生效", Formatters.time(c.notBefore))
                            InfoRow("到期", Formatters.time(c.notAfter))
                        }
                        SectionCard("主体") {
                            InfoRow("Subject", c.subject, mono = true)
                            InfoRow("Issuer", c.issuer, mono = true)
                            InfoRow("序列号", c.serial, mono = true)
                            InfoRow("签名算法", c.sigAlg, mono = true)
                        }
                        if (c.sans.isNotEmpty()) {
                            SectionCard("SAN (${c.sans.size})") {
                                c.sans.take(30).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface) }
                            }
                        }
                        SectionCard("指纹") { CodeBlock(c.sha256) }
                    }
                }
            }
        }
    }
}

private val dnsServers = listOf(
    "223.5.5.5" to "阿里 DNS",
    "119.29.29.29" to "腾讯 DNS",
    "1.1.1.1" to "Cloudflare",
    "8.8.8.8" to "Google",
    "9.9.9.9" to "Quad9",
    "180.76.76.76" to "百度 DNS"
)

@Composable
fun DnsBenchScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("github.com") }
    var running by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<DnsClient.DnsResult>() }
    val scope = rememberCoroutineScope()

    ScreenScaffold("DNS 基准测试", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("域名") })
            Button(
                onClick = {
                    results.clear()
                    running = true
                    scope.launch {
                        dnsServers.forEach { (server, _) ->
                            val r = withContext(Dispatchers.IO) { DnsClient.query(server, host.trim()) }
                            results.add(r)
                        }
                        running = false
                    }
                },
                enabled = !running && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "测试中…" else "开始测试") }
            SectionCard("结果") {
                if (results.isEmpty()) {
                    Text("依次向各公共 DNS 发起查询并计时。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val fastest = results.filter { it.error == null }.minByOrNull { it.timeMs }
                    results.forEach { r ->
                        val name = dnsServers.firstOrNull { it.first == r.server }?.second ?: r.server
                        val isFastest = fastest?.server == r.server
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "$name (${r.server})" + if (isFastest) "  ⚡最快" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isFastest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    r.addresses.take(2).joinToString(", ").ifBlank { r.error ?: "-" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${r.timeMs} ms", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
