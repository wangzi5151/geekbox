@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.devtools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.util.Codecs
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.core.util.JsonFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Composable
fun JsonToolScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun apply(min: Boolean) {
        if (input.isBlank()) { output = ""; error = null; return }
        runCatching { if (min) JsonFormatter.minify(input) else JsonFormatter.pretty(input) }
            .onSuccess { output = it; error = null }
            .onFailure { error = it.message ?: "解析失败"; output = "" }
    }

    ScreenScaffold("JSON", onBack, actions = {
        IconButton(onClick = { input = ""; output = ""; error = null }) {
            Icon(Icons.Filled.Refresh, contentDescription = "清空")
        }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 12,
                placeholder = { Text("粘贴 JSON") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { apply(false) }) { Text("格式化") }
                OutlinedButton(onClick = { apply(true) }) { Text("压缩") }
                OutlinedButton(
                    onClick = {
                        error = if (input.isBlank()) "输入为空" else JsonFormatter.validate(input)
                        output = ""
                    }
                ) { Text("校验") }
            }
            val status = when {
                error != null -> error!!
                input.isBlank() -> "等待输入…"
                output.isNotEmpty() -> "有效 JSON · ${output.length} 字符"
                else -> ""
            }
            SectionCard("结果") {
                if (output.isNotEmpty()) {
                    CodeBlock(output)
                } else {
                    Text(
                        status,
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun digestStream(stream: java.io.InputStream): List<Pair<String, String>> {
    val algorithms = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")
    val digests = algorithms.map { MessageDigest.getInstance(it) }
    val buffer = ByteArray(16384)
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        digests.forEach { it.update(buffer, 0, read) }
    }
    return algorithms.mapIndexed { i, a -> a to digests[i].digest().joinToString("") { "%02x".format(it) } }
}

@Composable
fun HashScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileHashes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        fileName = uri.lastPathSegment ?: "file"
        fileHashes = emptyList()
        fileError = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { digestStream(it) } ?: error("无法读取文件")
                }
            }.onSuccess { fileHashes = it }
                .onFailure { fileError = it.message }
        }
    }

    ScreenScaffold("Hash", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                placeholder = { Text("输入文本") }
            )
            SectionCard("文本哈希") {
                if (input.isEmpty()) {
                    Text("等待输入…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Codecs.hashes(input).forEach { (algo, value) ->
                        Text(algo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        CodeBlock(value)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            SectionCard("文件哈希") {
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("选择文件")
                }
                if (fileName != null) {
                    Spacer(Modifier.height(10.dp))
                    InfoRow("文件", fileName ?: "-", mono = true)
                    fileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    fileHashes.forEach { (algo, value) ->
                        Text(algo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        CodeBlock(value)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RegexScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(false) }
    var multiline by remember { mutableStateOf(false) }

    val regexResult = remember(pattern, input, ignoreCase, multiline) {
        if (pattern.isEmpty()) null
        else runCatching {
            var options = setOf<RegexOption>()
            if (ignoreCase) options = options + RegexOption.IGNORE_CASE
            if (multiline) options = options + RegexOption.MULTILINE
            Regex(pattern, options).findAll(input).toList()
        }
    }
    val matches = regexResult?.getOrNull().orEmpty()
    val error = regexResult?.exceptionOrNull()?.message

    ScreenScaffold("正则表达式", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("正则表达式") },
                placeholder = { Text("(\\d+)-(\\d+)") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text("忽略大小写") })
                FilterChip(selected = multiline, onClick = { multiline = !multiline }, label = { Text("多行") })
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                label = { Text("测试文本") }
            )
            val clipboard = LocalClipboardManager.current
            SectionCard("匹配 (${matches.size})") {
                when {
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    pattern.isEmpty() -> Text("输入正则后自动匹配", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    matches.isEmpty() -> Text("无匹配", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> matches.forEachIndexed { index, m ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("匹配 #${index + 1}  [${m.range.first}..${m.range.last}]", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(m.value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                                if (m.groupValues.size > 1) {
                                    Text(
                                        m.groupValues.drop(1).mapIndexed { i, g -> "g$i=$g" }.joinToString("  "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(m.value)) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TimestampScreen(onBack: () -> Unit) {
    var epochInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }

    val epochText = remember(epochInput) {
        epochInput.filter { it.isDigit() || it == '-' }.toLongOrNull()
            ?.let { seconds -> runCatching { Formatters.time(seconds * 1000, "yyyy-MM-dd HH:mm:ss") }.getOrNull() }
    }
    val dateEpoch = remember(dateInput) {
        if (dateInput.isBlank()) null
        else runCatching { Codecs.textToEpoch(dateInput) }.getOrNull()
    }

    ScreenScaffold("时间戳", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("当前时间") {
                val now = System.currentTimeMillis()
                InfoRow("秒", (now / 1000).toString(), mono = true)
                InfoRow("毫秒", now.toString(), mono = true)
                InfoRow("日期", Formatters.time(now))
            }
            SectionCard("Epoch → 时间") {
                OutlinedTextField(
                    value = epochInput,
                    onValueChange = { epochInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Unix 秒") },
                    placeholder = { Text("1700000000") }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    epochText ?: "等待有效输入…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (epochText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionCard("时间 → Epoch") {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("yyyy-MM-dd HH:mm:ss") },
                    placeholder = { Text("2026-01-01 00:00:00") }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    dateEpoch?.toString() ?: "无法解析",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (dateEpoch != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UuidScreen(onBack: () -> Unit) {
    var count by remember { mutableStateOf(1) }
    var uuids by remember { mutableStateOf(listOf(Codecs.uuid())) }
    val clipboard = LocalClipboardManager.current

    ScreenScaffold("UUID", onBack, actions = {
        IconButton(onClick = { uuids = List(count) { Codecs.uuid() } }) {
            Icon(Icons.Filled.Refresh, contentDescription = "生成")
        }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 10, 20).forEach { n ->
                    FilterChip(selected = count == n, onClick = { count = n; uuids = List(n) { Codecs.uuid() } }, label = { Text("x$n") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { uuids = List(count) { Codecs.uuid() } }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("生成")
                }
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(uuids.joinToString("\n"))) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制全部")
                }
            }
            SectionCard("结果 (${uuids.size})") {
                uuids.forEach { u ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(u, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(u)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JwtScreen(onBack: () -> Unit) {
    var token by remember { mutableStateOf("") }

    val parsed = remember(token) {
        if (token.isBlank()) null
        else runCatching {
            val parts = token.trim().split(".")
            require(parts.size >= 2) { "JWT 至少需要 Header.Payload 两段" }
            val header = JsonFormatter.pretty(Codecs.base64UrlDecodeToString(parts[0]))
            val payload = JsonFormatter.pretty(Codecs.base64UrlDecodeToString(parts[1]))
            Triple(header, payload, parts.getOrElse(2) { "" })
        }
    }

    ScreenScaffold("JWT", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                label = { Text("JWT Token") },
                placeholder = { Text("eyJhbGciOi...") }
            )
            val error = parsed?.exceptionOrNull()?.message
            if (error != null) {
                SectionCard { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            parsed?.getOrNull()?.let { (header, payload, signature) ->
                SectionCard("Header") { CodeBlock(header) }
                SectionCard("Payload") { CodeBlock(payload) }
                SectionCard("Signature") {
                    Text(signature.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("仅解码，不验证签名", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
