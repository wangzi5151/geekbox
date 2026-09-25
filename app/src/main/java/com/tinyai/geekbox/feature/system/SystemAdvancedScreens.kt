@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.system

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.shizuku.PrivilegedExec
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProcessManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    val shizukuAuth = remember(ShizukuBridge.revision) { ShizukuBridge.isAuthorized() }
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<ProcessEntry?>(null) }
    val scope = rememberCoroutineScope()
    val state = rememberAsync(refresh, shizukuAuth) {
        if (shizukuAuth) repo.processesViaShizuku() else repo.processes()
    }

    ScreenScaffold("进程管理器", onBack, actions = {
        IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "执行权限：${PrivilegedExec.modeLabel()}${if (shizukuAuth) "" else "（授权 Shizuku 可读取完整列表并结束进程）"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索进程名 / PID") }
            )
            AsyncContent(state) { list ->
                val filtered = list.filter { query.isBlank() || it.name.contains(query, true) || it.pid.toString().contains(query) }
                Text("共 ${filtered.size} 个进程", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered) { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.pid}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(64.dp))
                            Text(p.name, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (PrivilegedExec.available()) {
                                IconButton(onClick = { pending = p }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "结束", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("结束进程") },
            text = { Text("确定结束 PID ${p.pid}（${p.name}）吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val proc = p
                    pending = null
                    scope.launch {
                        withContext(Dispatchers.IO) { PrivilegedExec.run("kill -9 ${proc.pid}", 10) }
                        refresh++
                    }
                }) { Text("结束") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } }
        )
    }
}

private val dumpsysPresets = listOf(
    "battery", "power", "wifi", "connectivity", "package", "activity",
    "meminfo", "netstats", "display", "input", "alarm", "jobscheduler", "window", "audio", "usb"
)

@Composable
fun DumpsysScreen(onBack: () -> Unit) {
    var service by remember { mutableStateOf("battery") }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("dumpsys 浏览器", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "执行权限：${PrivilegedExec.modeLabel()}（完整输出需 Shizuku 或 Root）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(value = service, onValueChange = { service = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("服务名") })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dumpsysPresets.take(5).forEach { s ->
                    FilterChip(selected = service == s, onClick = { service = s }, label = { Text(s) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dumpsysPresets.drop(5).take(5).forEach { s ->
                    FilterChip(selected = service == s, onClick = { service = s }, label = { Text(s) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dumpsysPresets.drop(10).forEach { s ->
                    FilterChip(selected = service == s, onClick = { service = s }, label = { Text(s) })
                }
            }
            Button(
                onClick = {
                    running = true
                    val svc = service
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { PrivilegedExec.run("dumpsys $svc", 20) }
                        val text = result.combined.ifBlank { "无输出（可能权限不足）" }
                        output = if (text.length > 20000) text.take(20000) + "\n…(已截断)" else text
                        running = false
                    }
                },
                enabled = !running && service.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "执行中…" else "执行 dumpsys")
            }
            if (output.isNotBlank()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(output, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
