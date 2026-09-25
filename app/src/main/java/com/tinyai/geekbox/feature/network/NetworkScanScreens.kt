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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.StatBar
import kotlinx.coroutines.launch

private val commonPorts = listOf(
    21, 22, 23, 25, 53, 67, 68, 69, 80, 110, 123, 135, 137, 138, 139, 143, 161, 389, 443,
    445, 465, 514, 515, 587, 631, 993, 995, 1080, 1433, 1521, 1723, 2049, 2181, 3000, 3306,
    3389, 4444, 5000, 5432, 5555, 5900, 6379, 6667, 7000, 8000, 8080, 8081, 8443, 8888, 9000,
    9090, 9200, 11211, 27017
)

@Composable
fun PortScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var host by remember { mutableStateOf(repo.currentIpv4() ?: "127.0.0.1") }
    var full by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val open = remember { mutableStateListOf<Int>() }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("端口扫描", onBack) { padding ->
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
                FilterChip(selected = !full, onClick = { full = false }, label = { Text("常见端口") })
                FilterChip(selected = full, onClick = { full = true }, label = { Text("1 - 1024") })
            }
            Button(
                onClick = {
                    val ports = if (full) (1..1024).toList() else commonPorts
                    open.clear()
                    done = 0
                    total = ports.size
                    running = true
                    scope.launch {
                        runCatching {
                            repo.portScan(host.trim(), ports, 600, onFound = { open.add(it) }) { d, _ -> done = d }
                        }
                        running = false
                    }
                },
                enabled = !running && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "扫描中… ($done/$total)" else "开始扫描")
            }
            if (total > 0) {
                StatBar(if (total == 0) 0f else done.toFloat() / total.toFloat())
            }
            SectionCard("开放端口 (${open.size})") {
                if (open.isEmpty()) {
                    Text(
                        if (running) "扫描中…" else "未发现开放端口",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    open.sorted().forEach { p ->
                        InfoRow("$p", serviceName(p), mono = true, valueColor = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

private fun serviceName(port: Int): String = when (port) {
    21 -> "FTP"
    22 -> "SSH"
    23 -> "Telnet"
    25 -> "SMTP"
    53 -> "DNS"
    80 -> "HTTP"
    110 -> "POP3"
    143 -> "IMAP"
    443 -> "HTTPS"
    445 -> "SMB"
    587 -> "SMTP/TLS"
    993 -> "IMAPS"
    1433 -> "MSSQL"
    3306 -> "MySQL"
    3389 -> "RDP"
    5432 -> "PostgreSQL"
    6379 -> "Redis"
    8080 -> "HTTP-Alt"
    8443 -> "HTTPS-Alt"
    27017 -> "MongoDB"
    else -> "open"
}

@Composable
fun LanScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var running by remember { mutableStateOf(false) }
    val hosts = remember { mutableStateListOf<String>() }
    var done by remember { mutableIntStateOf(0) }
    val subnet = remember { repo.currentSubnet() }
    val scope = rememberCoroutineScope()

    ScreenScaffold("局域网扫描", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("网段") {
                InfoRow("本机 IPv4", repo.currentIpv4() ?: "-", mono = true)
                InfoRow("扫描范围", subnet?.let { "$it.1 - $it.254" } ?: "不可用", mono = true)
            }
            Button(
                onClick = {
                    hosts.clear()
                    done = 0
                    running = true
                    scope.launch {
                        runCatching {
                            repo.lanHosts(onFound = { hosts.add(it) }, onProgress = { done = it })
                        }
                        running = false
                    }
                },
                enabled = !running && subnet != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "扫描中… ($done/254)" else "开始扫描")
            }
            StatBar(done.toFloat() / 254f)
            SectionCard("在线主机 (${hosts.size})") {
                if (hosts.isEmpty()) {
                    Text(if (running) "扫描中…" else "未发现在线主机", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    hosts.sortedBy { it.split(".").last().toIntOrNull() ?: 0 }
                        .forEach { InfoRow(it, "在线", mono = true, valueColor = MaterialTheme.colorScheme.tertiary) }
                }
            }
        }
    }
}
