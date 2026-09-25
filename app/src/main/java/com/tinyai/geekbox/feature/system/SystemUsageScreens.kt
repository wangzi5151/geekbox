@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.system

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.shizuku.PrivilegedExec
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.feature.apps.AppEntry
import com.tinyai.geekbox.feature.apps.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppAction(val title: String, val command: (String) -> String)

private val appActions = listOf(
    AppAction("强制停止") { "am force-stop $it" },
    AppAction("冻结 (停用)") { "pm disable-user --user 0 $it" },
    AppAction("启用") { "pm enable $it" },
    AppAction("清除数据") { "pm clear $it" },
    AppAction("卸载") { "pm uninstall --user 0 $it" }
)

@Composable
fun AppManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val state = rememberAsync { repo.listApps(false) }

    ScreenScaffold("应用管理", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "执行权限：${PrivilegedExec.modeLabel()}（操作应用需 Shizuku 或 Root）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("搜索应用") })
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    val list = s.value.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list, key = { it.packageName }) { app ->
                            androidx.compose.material3.Card(
                                modifier = Modifier.fillMaxWidth().clickable { selected = app },
                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(app.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(app.packageName, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(app.label) },
            text = {
                Column {
                    Text(app.packageName, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    appActions.forEach { action ->
                        Text(
                            action.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val pkg = app.packageName
                                    val cmd = action.command(pkg)
                                    selected = null
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) { PrivilegedExec.run(cmd, 15) }
                                        val msg = result.combined.ifBlank { "${action.title} 完成" }
                                        Toast.makeText(context, msg.take(120), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (action.title == "卸载") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } }
        )
    }
}

@Composable
fun AppStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val usageRepo = remember { AppUsageRepository(context) }
    var hasAccess by remember { mutableStateOf(usageRepo.hasUsageAccess()) }
    var refresh by remember { mutableStateOf(0) }
    val state = rememberAsync(hasAccess, refresh) {
        if (hasAccess) usageRepo.storageByUid() else emptyList()
    }

    ScreenScaffold("应用存储占用", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!hasAccess) {
                SectionCard("需要「使用情况访问」权限") {
                    Text("读取应用存储占用需要授予「使用情况访问」。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("打开设置") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { hasAccess = usageRepo.hasUsageAccess() }, modifier = Modifier.fillMaxWidth()) { Text("我已授权，重新检测") }
                }
            } else {
                when (val s = state) {
                    is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                    is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                    is Async.Success -> {
                        SectionCard("合计") {
                            Text("总占用 ${Formatters.bytes(s.value.sumOf { it.total })}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(s.value) { st ->
                                androidx.compose.material3.Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(usageRepo.labelForUid(st.uid), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                            Text(Formatters.bytes(st.total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "数据 ${Formatters.bytes(st.dataBytes)} · 缓存 ${Formatters.bytes(st.cacheBytes)}",
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
