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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.StatBar
import com.tinyai.geekbox.core.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun WhoisScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var target by remember { mutableStateOf("github.com") }
    var running by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    ScreenScaffold("WHOIS 查询", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = target, onValueChange = { target = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("域名或 IP") })
            Button(
                onClick = {
                    running = true
                    output = ""
                    val t = target
                    scope.launch {
                        output = withContext(Dispatchers.IO) { repo.whois(t) }
                        running = false
                    }
                },
                enabled = !running && target.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "查询中…" else "查询")
            }
            if (output.isNotBlank()) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(output)) }, modifier = Modifier.fillMaxWidth()) { Text("复制结果") }
                SectionCard("结果") {
                    Text(output, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun DownloaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(-1L) }
    var message by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val startTime = remember { longArrayOf(0L) }

    ScreenScaffold("下载器", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL (http/https)") })
            OutlinedTextField(value = fileName, onValueChange = { fileName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("保存文件名 (可选)") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        running = true
                        downloaded = 0; total = -1; message = null
                        startTime[0] = System.currentTimeMillis()
                        val u = url
                        val name = fileName.ifBlank { u.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" } }
                        job = scope.launch {
                            runCatching {
                                val dir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
                                val target = File(dir, name)
                                repo.download(u, target) { d, t ->
                                    downloaded = d
                                    total = t
                                }
                                target.absolutePath
                            }.onSuccess { message = "已保存: $it" }
                                .onFailure { message = "失败: ${it.message}" }
                            running = false
                        }
                    },
                    enabled = !running && url.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (running) "下载中…" else "开始下载")
                }
                OutlinedButton(onClick = { job?.cancel(); running = false }, enabled = running, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("取消")
                }
            }
            SectionCard("进度") {
                val fraction = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                val elapsed = ((System.currentTimeMillis() - startTime[0]).coerceAtLeast(1)) / 1000.0
                val speed = if (downloaded > 0 && startTime[0] > 0) (downloaded / elapsed).toLong() else 0L
                if (total > 0) StatBar(fraction)
                Spacer(Modifier.height(6.dp))
                Text(
                    "已下载 ${Formatters.bytes(downloaded)}" + if (total > 0) " / ${Formatters.bytes(total)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("速度 ${Formatters.bytes(speed)}/s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            message?.let { SectionCard { Text(it, color = MaterialTheme.colorScheme.onSurface) } }
            SectionCard {
                Text(
                    "仅在你主动发起时下载你指定的 URL，保存到应用外部目录 downloads/。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
