@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.ActionRow
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ModuleHeader
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.EmptyState
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Filled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AppsHomeScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var includeSystem by rememberSaveable { mutableStateOf(false) }
    val state = rememberAsync(includeSystem) { repo.listApps(includeSystem) }

    ScreenScaffold("GeekBox", onBack = null) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ModuleHeader(
                    icon = Icons.Filled.Android,
                    title = "应用",
                    subtitle = "已安装应用 · APK 深度分析"
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("搜索应用名或包名") }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !includeSystem, onClick = { includeSystem = false }, label = { Text("用户应用") })
                    FilterChip(selected = includeSystem, onClick = { includeSystem = true }, label = { Text("包含系统") })
                }
                Spacer(Modifier.height(10.dp))
                ActionRow(
                    title = "分析 APK 文件",
                    subtitle = "选择本地 APK 查看清单/权限/签名",
                    icon = Icons.Filled.FileOpen,
                    onClick = { onOpen("apps/apk") }
                )
            }
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    val filtered = s.value.filter {
                        query.isBlank() ||
                            it.label.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyState("没有匹配的应用")
                    } else {
                        Text(
                            "共 ${filtered.size} 个应用",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.packageName }) { app ->
                                androidx.compose.material3.Card(
                                    onClick = { onOpen("apps/detail/${app.packageName}") },
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIcon(app.packageName)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(app.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                            Spacer(Modifier.height(2.dp))
                                            Text(app.packageName, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "v${app.versionName} (${app.versionCode}) · ${Formatters.bytes(app.sizeBytes)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (app.isSystem) {
                                            Text("系统", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
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
}

@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    val state = rememberAsync(packageName) { repo.details(packageName) }

    ScreenScaffold("应用详情", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { d ->
                AppActions(d) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                AppDetailsContent(d)
            }
        }
    }
}

@Composable
private fun AppActions(d: AppDetails, toast: (String) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val intent = context.packageManager.getLaunchIntentForPackage(d.packageName)
            if (intent != null) context.startActivity(intent) else toast("无法启动该应用")
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("打开")
        }
        Button(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${d.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { toast("无法打开系统设置") }
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("信息")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val source = File(d.apkPath)
            if (!source.exists()) { toast("APK 路径不可读"); return@Button }
            runCatching {
                val dir = File(context.getExternalFilesDir(null), "exported").apply { mkdirs() }
                val target = File(dir, "${d.label}-${d.versionName}.apk")
                source.copyTo(target, overwrite = true)
                toast("已导出: ${target.absolutePath}")
            }.onFailure { toast("导出失败: ${it.message}") }
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("导出APK")
        }
        Button(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:${d.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { toast("无法发起卸载") }
        }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("卸载")
        }
    }
}

@Composable
private fun AppDetailsContent(d: AppDetails) {
    SectionCard("基本信息") {
        InfoRow("名称", d.label)
        InfoRow("包名", d.packageName, mono = true)
        InfoRow("版本", "${d.versionName} (${d.versionCode})")
        InfoRow("Target SDK", d.targetSdk.toString())
        InfoRow("Min SDK", d.minSdk.toString())
        InfoRow("UID", d.uid.toString(), mono = true)
        InfoRow("类型", if (d.isSystem) "系统应用" else "用户应用")
        InfoRow("调试", if (d.isDebug) "可调试" else "不可调试")
        InfoRow("启用", if (d.enabled) "是" else "否")
        InfoRow("安装位置", d.installLocation)
        InfoRow("首次安装", Formatters.time(d.firstInstallTime))
        InfoRow("最近更新", Formatters.time(d.lastUpdateTime))
        InfoRow("APK 大小", Formatters.bytes(d.apkSize))
        InfoRow("ABI", d.abis.joinToString(", "), mono = true)
    }
    SectionCard("路径") {
        InfoRow("APK", d.apkPath, mono = true)
        InfoRow("数据目录", d.dataDir, mono = true)
        InfoRow("Native 库", d.nativeLibDir, mono = true)
    }
    if (d.signatures.isNotEmpty()) {
        SectionCard("签名") {
            d.signatures.forEachIndexed { i, sig ->
                if (d.signatures.size > 1) {
                    Text("签名 #${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                CodeBlock("SHA-256\n${sig.sha256}")
                Spacer(Modifier.height(8.dp))
                CodeBlock("SHA-1\n${sig.sha1}")
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    ComponentSection("Activity (${d.activities.size})", d.activities)
    ComponentSection("Service (${d.services.size})", d.services)
    ComponentSection("Receiver (${d.receivers.size})", d.receivers)
    ComponentSection("Provider (${d.providers.size})", d.providers)
    if (d.permissions.isNotEmpty()) {
        SectionCard("权限 (${d.permissions.count { it.granted }}/${d.permissions.size} 已授予)") {
            d.permissions.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        if (p.granted) "✓" else "✗",
                        color = if (p.granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        p.name.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentSection(title: String, components: List<ComponentItem>) {
    if (components.isEmpty()) return
    SectionCard(title) {
        components.forEach { c ->
            Column(Modifier.padding(vertical = 5.dp)) {
                Text(
                    c.name.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    c.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val tags = buildList {
                    if (c.exported) add("EXPORTED")
                    if (!c.enabled) add("DISABLED")
                    c.permission?.let { add("perm=$it") }
                }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (c.exported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ApkInspectScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    var apkPath by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val target = File(context.cacheDir, "inspect.apk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("无法读取文件")
            apkPath = target.absolutePath
            fileName = uri.lastPathSegment ?: "apk"
            error = null
        }.onFailure {
            error = it.message ?: "读取失败"
        }
    }

    val state = rememberAsync(apkPath) {
        apkPath?.let { repo.archiveDetails(it) }
    }

    ScreenScaffold(
        "APK 分析",
        onBack,
        actions = {
            IconButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.FileOpen, contentDescription = "选择 APK")
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("文件") {
                InfoRow("名称", fileName ?: "未选择")
                InfoRow("路径", apkPath ?: "-", mono = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("选择 APK 文件")
                }
            }
            if (error != null) {
                SectionCard("错误") { Text(error!!, color = MaterialTheme.colorScheme.error) }
            }
            if (apkPath != null) {
                when (val s = state) {
                    is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                    is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                    is Async.Success -> {
                        val d = s.value
                        if (d == null) {
                            SectionCard { Text("无法解析该 APK。", color = MaterialTheme.colorScheme.error) }
                        } else {
                            SectionCard("分析结果") {
                                Text("来自 APK 文件，非已安装应用", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            AppDetailsContent(d)
                        }
                    }
                }
            }
        }
    }
}
