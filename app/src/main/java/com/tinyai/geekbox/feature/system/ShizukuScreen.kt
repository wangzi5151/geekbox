@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.system

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard

private const val REQUEST_CODE = 4001

@Composable
fun ShizukuScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val revision = ShizukuBridge.revision
    var bump by remember { mutableStateOf(0) }

    val installed = remember(revision, bump) { ShizukuBridge.isInstalled(context) }
    val alive = remember(revision, bump) { ShizukuBridge.binderAlive() }
    val authorized = remember(revision, bump) { ShizukuBridge.isAuthorized() }
    val version = remember(revision, bump) { ShizukuBridge.version() }
    val uidLabel = remember(revision, bump) { ShizukuBridge.uidLabel() }
    val preV11 = remember(revision, bump) { ShizukuBridge.isPreV11() }

    ScreenScaffold("Shizuku", onBack, actions = {
        IconButton(onClick = { bump++ }) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("状态") {
                InfoRow("已安装", if (installed) "是" else "否", valueColor = if (installed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                InfoRow("Binder", if (alive) "运行中" else "未运行", valueColor = if (alive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                InfoRow("授权", if (authorized) "已授权" else "未授权", valueColor = if (authorized) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                InfoRow("版本", version?.toString() ?: "-")
                InfoRow("身份", uidLabel, mono = true)
                if (preV11) InfoRow("兼容", "版本过低 (pre-v11)", valueColor = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { ShizukuBridge.requestPermission(REQUEST_CODE) },
                    enabled = installed && alive && !authorized && !preV11,
                    modifier = Modifier.weight(1f)
                ) { Text("请求授权") }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.packageManager.getLaunchIntentForPackage(ShizukuBridge.PACKAGE)
                                ?.let { context.startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    },
                    enabled = installed,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("打开")
                }
            }

            SectionCard("如何启用") {
                val steps = buildList {
                    if (!installed) {
                        add("1. 安装 Shizuku 应用（shizuku.rikka.app）")
                        add("2. 通过无线调试或 Root 启动 Shizuku 服务")
                        add("3. 回到本页点击「请求授权」")
                    } else if (!alive) {
                        add("1. 打开 Shizuku 应用")
                        add("2. 通过无线调试或 Root 启动服务")
                        add("3. 回到本页点击「请求授权」")
                    } else if (!authorized) {
                        add("点击「请求授权」，在弹窗中允许 GeekBox 使用 Shizuku。")
                    } else {
                        add("Shizuku 已就绪：Logcat、进程、Shell 将以 shell 身份运行。")
                    }
                }
                steps.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Shizuku 让普通应用以 ADB(shell) 身份调用系统能力，无需 Root。GeekBox 仅在授权后使用它执行你主动发起的命令。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
