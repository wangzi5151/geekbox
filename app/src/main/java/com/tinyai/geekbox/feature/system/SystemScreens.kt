@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.system

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.ActionRow
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogcatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    val rootStatus = remember { runCatching { repo.rootStatus() }.getOrNull() }
    val hasSu = rootStatus?.suPath != null
    val shizukuAuth = remember(ShizukuBridge.revision) { ShizukuBridge.isAuthorized() }

    var source by remember(shizukuAuth) {
        mutableStateOf(
            when {
                shizukuAuth -> LogSource.SHIZUKU
                hasSu -> LogSource.ROOT
                else -> LogSource.LOCAL
            }
        )
    }
    var follow by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<String>() }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    ScreenScaffold(
        "Logcat",
        onBack,
        actions = {
            IconButton(onClick = { lines.clear() }) { Icon(Icons.Filled.Clear, contentDescription = "清空") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("日志来源") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = source == LogSource.SHIZUKU, onClick = { source = LogSource.SHIZUKU }, enabled = shizukuAuth, label = { Text("Shizuku") })
                    FilterChip(selected = source == LogSource.ROOT, onClick = { source = LogSource.ROOT }, enabled = hasSu, label = { Text("Root") })
                    FilterChip(selected = source == LogSource.LOCAL, onClick = { source = LogSource.LOCAL }, label = { Text("本应用") })
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        source == LogSource.SHIZUKU -> "以 shell 身份读取完整日志（无需 Root）"
                        source == LogSource.ROOT -> "使用 su 读取系统日志"
                        else -> "仅能读取本应用相关日志（受系统限制）"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = follow, onClick = { follow = true }, label = { Text("实时") })
                FilterChip(selected = !follow, onClick = { follow = false }, label = { Text("快照 500 行") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        running = true
                        job = scope.launch {
                            runCatching {
                                val cmd = if (follow) "logcat -v time -T 1" else "logcat -v time -t 500"
                                when (source) {
                                    LogSource.SHIZUKU -> ShizukuBridge.streamShell(cmd) { line ->
                                        lines.add(line)
                                        if (lines.size > 3000) lines.removeAt(0)
                                    }
                                    LogSource.ROOT -> repo.logcat(true, false, 500, follow) { line ->
                                        lines.add(line)
                                        if (lines.size > 3000) lines.removeAt(0)
                                    }
                                    LogSource.LOCAL -> repo.logcat(false, false, 500, follow) { line ->
                                        lines.add(line)
                                        if (lines.size > 3000) lines.removeAt(0)
                                    }
                                }
                            }.onFailure { lines.add("错误: ${it.message}") }
                            running = false
                        }
                    },
                    enabled = !running,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (running) "运行中…" else "开始")
                }
                OutlinedButton(
                    onClick = { job?.cancel(); running = false },
                    enabled = running,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("停止")
                }
            }
            if (lines.isEmpty()) {
                SectionCard { Text("暂无日志。点击开始抓取。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(lines) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private enum class LogSource { SHIZUKU, ROOT, LOCAL }

@Composable
fun ShellScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    val hasSu = remember { runCatching { repo.rootStatus().suPath != null }.getOrDefault(false) }
    val shizukuAuth = remember(ShizukuBridge.revision) { ShizukuBridge.isAuthorized() }
    var source by remember(shizukuAuth) {
        mutableStateOf(
            when {
                shizukuAuth -> ExecSource.SHIZUKU
                hasSu -> ExecSource.ROOT
                else -> ExecSource.LOCAL
            }
        )
    }
    var command by remember { mutableStateOf("getprop ro.product.model") }
    var running by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var exitInfo by remember { mutableStateOf<String?>(null) }
    var showExamples by remember { mutableStateOf(false) }
    var libraryQuery by remember { mutableStateOf("") }
    var libraryCategory by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val outputScroll = rememberScrollState()

    LaunchedEffect(output) {
        outputScroll.animateScrollTo(outputScroll.maxValue)
    }

    ScreenScaffold("Shell", onBack, actions = {
        IconButton(onClick = { output = ""; exitInfo = null }) { Icon(Icons.Filled.Clear, contentDescription = "清空") }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = source == ExecSource.SHIZUKU, onClick = { source = ExecSource.SHIZUKU }, enabled = shizukuAuth, label = { Text("Shizuku") })
                FilterChip(selected = source == ExecSource.ROOT, onClick = { source = ExecSource.ROOT }, enabled = hasSu, label = { Text("Root") })
                FilterChip(selected = source == ExecSource.LOCAL, onClick = { source = ExecSource.LOCAL }, label = { Text("本地") })
            }
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = { Text("命令") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        running = true
                        output = ""
                        exitInfo = null
                        val cmd = command
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                when (source) {
                                    ExecSource.SHIZUKU -> ShizukuBridge.runShell(cmd)
                                    ExecSource.ROOT -> com.tinyai.geekbox.core.util.Shell.runRoot(cmd)
                                    ExecSource.LOCAL -> com.tinyai.geekbox.core.util.Shell.run(cmd)
                                }
                            }
                            output = buildString {
                                append("$ ")
                                append(cmd)
                                append("\n\n")
                                if (result.combined.isNotBlank()) append(result.combined)
                            }
                            exitInfo = "exit=${result.exitCode}" + if (result.timedOut) " · 超时" else ""
                            running = false
                        }
                    },
                    enabled = !running && command.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (running) "执行中…" else "执行")
                }
                OutlinedButton(onClick = { showExamples = true }, modifier = Modifier.weight(1f)) {
                    Text("命令库 (${ShellLibrary.totalCount})")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("输出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                val info = exitInfo
                if (info != null) {
                    Text(info, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            androidx.compose.material3.Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (output.isBlank()) {
                    Text(
                        "等待执行…",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(outputScroll)
                            .padding(14.dp)
                    ) {
                        Text(
                            output,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showExamples) {
        Dialog(
            onDismissRequest = { showExamples = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("命令库 (${ShellLibrary.totalCount})", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showExamples = false }) { Icon(Icons.Filled.Clear, contentDescription = "关闭") }
                    }
                    OutlinedTextField(
                        value = libraryQuery,
                        onValueChange = { libraryQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索命令") }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = libraryCategory == null, onClick = { libraryCategory = null }, label = { Text("全部") })
                        }
                        items(ShellLibrary.categories) { cat ->
                            FilterChip(
                                selected = libraryCategory == cat.name,
                                onClick = { libraryCategory = cat.name },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    val list = when {
                        libraryQuery.isNotBlank() -> ShellLibrary.search(libraryQuery)
                        libraryCategory != null -> ShellLibrary.categories.firstOrNull { it.name == libraryCategory }?.commands ?: emptyList()
                        else -> ShellLibrary.categories.flatMap { it.commands }
                    }
                    Text("${list.size} 条命令", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(list) { cmd ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { command = cmd.command; showExamples = false }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(cmd.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    cmd.command,
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

private enum class ExecSource { SHIZUKU, ROOT, LOCAL }

@Composable
fun IntentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var uriText by remember { mutableStateOf("https://example.com/path?id=1&name=geekbox#frag") }
    val parsed = remember(uriText) { runCatching { Uri.parse(uriText.trim()) }.getOrNull() }
    val handlers = remember(uriText) {
        if (parsed == null || uriText.isBlank()) emptyList()
        else runCatching {
            val intent = Intent(Intent.ACTION_VIEW, parsed)
            context.packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
        }.getOrDefault(emptyList())
    }

    ScreenScaffold("Intent / URI", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uriText,
                onValueChange = { uriText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = { Text("URI") }
            )
            if (parsed != null && uriText.isNotBlank()) {
                SectionCard("解析") {
                    InfoRow("Scheme", parsed.scheme ?: "-", mono = true)
                    InfoRow("Authority", parsed.authority ?: "-", mono = true)
                    InfoRow("Host", parsed.host ?: "-", mono = true)
                    InfoRow("Port", parsed.port.takeIf { it != -1 }?.toString() ?: "-", mono = true)
                    InfoRow("Path", parsed.path ?: "-", mono = true)
                    InfoRow("Fragment", parsed.fragment ?: "-", mono = true)
                    if (parsed.queryParameterNames.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Query", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        parsed.queryParameterNames.forEach { key ->
                            InfoRow(key, parsed.getQueryParameter(key) ?: "", mono = true)
                        }
                    }
                }
            }
            SectionCard("可处理 ACTION_VIEW 的应用 (${handlers.size})") {
                if (handlers.isEmpty()) Text("无匹配应用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else handlers.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface) }
            }
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                enabled = parsed != null && uriText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("用默认应用打开")
            }
        }
    }
}

@Composable
fun RootScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    val state = rememberAsync { repo.rootStatus() }
    var granted by remember { mutableStateOf<Boolean?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("Root 能力", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { r ->
                SectionCard("状态") {
                    InfoRow("su 文件", if (r.suPath != null) "存在" else "未找到", valueColor = if (r.suPath != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    InfoRow("su 路径", r.suPath ?: "-", mono = true)
                    InfoRow("授权结果", granted?.let { if (it) "已获取 Root" else "未授权" } ?: "未测试",
                        valueColor = when (granted) {
                            true -> MaterialTheme.colorScheme.tertiary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        })
                    InfoRow("SELinux", r.selinux ?: "-", mono = true)
                    InfoRow("Shizuku", if (r.shizukuInstalled) "已安装" else "未安装")
                }
                Button(
                    onClick = {
                        checking = true
                        scope.launch {
                            granted = withContext(Dispatchers.IO) { repo.requestRoot() }
                            checking = false
                        }
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (checking) "请求中…" else "测试 / 申请 Root 授权")
                }
                SectionCard("说明") {
                    Text(r.details, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GeekBox 不会静默提权：只有当你点击上方按钮或在 Logcat 中主动选择 Root 方式时，才会调用 su。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PropsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    var query by remember { mutableStateOf("") }
    val state = rememberAsync { repo.systemProperties() }
    ScreenScaffold("系统属性", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索属性") }
            )
            when (val s = state) {
                is com.tinyai.geekbox.core.ui.Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is com.tinyai.geekbox.core.ui.Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is com.tinyai.geekbox.core.ui.Async.Success -> {
                    val filtered = s.value.filter { query.isBlank() || it.first.contains(query, true) || it.second.contains(query, true) }
                    Text("共 ${filtered.size} 项", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtered) { (key, value) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(key, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    val shizukuAuth = remember(ShizukuBridge.revision) { ShizukuBridge.isAuthorized() }
    val state = rememberAsync(shizukuAuth) {
        if (shizukuAuth) repo.processesViaShizuku() else repo.processes()
    }
    ScreenScaffold("运行进程", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (shizukuAuth) "通过 Shizuku 以 shell 身份读取完整进程列表。"
                else "Android 9+ 对 /proc 访问有所限制，非 Shizuku/Root 可能只能看到部分进程。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (val s = state) {
                is com.tinyai.geekbox.core.ui.Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is com.tinyai.geekbox.core.ui.Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is com.tinyai.geekbox.core.ui.Async.Success -> {
                    Text("共 ${s.value.size} 个进程", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(s.value) { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text("${p.pid}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(64.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Shortcut(val title: String, val subtitle: String, val action: String)

private val shortcuts = listOf(
    Shortcut("开发者选项", "调试与 USB", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    Shortcut("无线调试", "配对与端口", Settings.ACTION_WIFI_SETTINGS),
    Shortcut("应用详情", "本应用设置", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
    Shortcut("WLAN", "网络设置", Settings.ACTION_WIFI_SETTINGS),
    Shortcut("蓝牙", "配对设备", Settings.ACTION_BLUETOOTH_SETTINGS),
    Shortcut("电池", "电量与省电", Settings.ACTION_BATTERY_SAVER_SETTINGS),
    Shortcut("存储", "空间管理", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
    Shortcut("位置", "定位服务", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    Shortcut("使用情况访问", "Usage Access", Settings.ACTION_USAGE_ACCESS_SETTINGS),
    Shortcut("辅助功能", "无障碍", Settings.ACTION_ACCESSIBILITY_SETTINGS),
    Shortcut("语言与输入", "语言设置", Settings.ACTION_LOCALE_SETTINGS),
    Shortcut("日期与时间", "时区设置", Settings.ACTION_DATE_SETTINGS),
    Shortcut("显示", "屏幕设置", Settings.ACTION_DISPLAY_SETTINGS),
    Shortcut("安全", "安全设置", Settings.ACTION_SECURITY_SETTINGS),
    Shortcut("VPN", "虚拟专用网", "android.net.vpn.SETTINGS"),
    Shortcut("通知使用权", "Notification Access", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
)

@Composable
fun ShortcutsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold("系统设置", onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shortcuts) { sc ->
                ActionRow(
                    title = sc.title,
                    subtitle = sc.subtitle,
                    icon = Icons.AutoMirrored.Filled.Launch,
                    onClick = {
                        runCatching {
                            val intent = if (sc.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                                Intent(sc.action, Uri.parse("package:${context.packageName}"))
                            } else {
                                Intent(sc.action)
                            }
                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                )
            }
        }
    }
}
