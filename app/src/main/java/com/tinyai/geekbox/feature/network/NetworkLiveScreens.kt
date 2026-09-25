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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.Sparkline
import com.tinyai.geekbox.core.ui.components.StatBar
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.feature.system.AppUsageRepository
import kotlinx.coroutines.delay

@Composable
fun NetworkSpeedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var downBps by remember { mutableStateOf(0L) }
    var upBps by remember { mutableStateOf(0L) }
    var totalRx by remember { mutableStateOf(0L) }
    var totalTx by remember { mutableStateOf(0L) }
    val downHist = remember { mutableStateListOf<Float>() }
    val upHist = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        var (lastRx, lastTx) = repo.trafficTotals()
        var lastTime = System.currentTimeMillis()
        while (true) {
            delay(1000)
            val (rx, tx) = repo.trafficTotals()
            val now = System.currentTimeMillis()
            val dt = ((now - lastTime).coerceAtLeast(1)) / 1000.0
            val d = ((rx - lastRx).coerceAtLeast(0) / dt).toLong()
            val u = ((tx - lastTx).coerceAtLeast(0) / dt).toLong()
            downBps = d
            upBps = u
            totalRx = rx
            totalTx = tx
            downHist.add(d / 1024f / 1024f)
            upHist.add(u / 1024f / 1024f)
            if (downHist.size > 60) downHist.removeAt(0)
            if (upHist.size > 60) upHist.removeAt(0)
            lastRx = rx
            lastTx = tx
            lastTime = now
        }
    }

    ScreenScaffold("网络速率", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("实时速率 (每秒)") {
                InfoRow("下载", "${Formatters.bytes(downBps)}/s", mono = true, valueColor = MaterialTheme.colorScheme.tertiary)
                InfoRow("上传", "${Formatters.bytes(upBps)}/s", mono = true, valueColor = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("下载", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                Sparkline(downHist.toList(), Modifier.fillMaxWidth().height(46.dp), color = MaterialTheme.colorScheme.tertiary, maxValue = (downHist.maxOrNull() ?: 1f).coerceAtLeast(0.05f))
                Spacer(Modifier.height(8.dp))
                Text("上传", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Sparkline(upHist.toList(), Modifier.fillMaxWidth().height(46.dp), color = MaterialTheme.colorScheme.primary, maxValue = (upHist.maxOrNull() ?: 1f).coerceAtLeast(0.05f))
            }
            SectionCard("自开机累计") {
                InfoRow("接收", Formatters.bytes(totalRx))
                InfoRow("发送", Formatters.bytes(totalTx))
                InfoRow("合计", Formatters.bytes(totalRx + totalTx))
            }
            SectionCard {
                Text(
                    "基于 TrafficStats 统计全设备流量，无需额外权限。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConnectionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    val usage = remember { AppUsageRepository(context) }
    var filter by remember { mutableStateOf("ALL") }
    var refresh by remember { mutableStateOf(0) }
    val state = rememberAsync(refresh) { repo.connections() }

    val shizuku = com.tinyai.geekbox.core.shizuku.ShizukuBridge.isAuthorized()

    ScreenScaffold("网络连接", onBack, actions = {
        IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (shizuku) "通过 Shizuku 读取系统连接表。" else "未授权 Shizuku 时可能只能看到部分连接（建议在「系统 → Shizuku」授权）。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "全部", "TCP" to "TCP", "UDP" to "UDP", "LISTEN" to "监听").forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                }
            }
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    val list = s.value.filter {
                        when (filter) {
                            "TCP" -> it.protocol.startsWith("TCP")
                            "UDP" -> it.protocol.startsWith("UDP")
                            "LISTEN" -> it.state == "LISTEN"
                            else -> true
                        }
                    }
                    Text("共 ${list.size} 条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list) { c ->
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth()) {
                                        Text("${c.protocol} · ${c.state}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                        Text(c.uid?.let { usage.labelForUid(it) } ?: "-", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("本地 ${c.local}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                                    Text("远端 ${c.remote}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
