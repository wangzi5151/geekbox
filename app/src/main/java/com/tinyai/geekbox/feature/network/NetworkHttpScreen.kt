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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import kotlinx.coroutines.launch

private val methods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")

@Composable
fun HttpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("https://httpbin.org/get") }
    var headers by remember { mutableStateOf("User-Agent: GeekBox\nAccept: application/json") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HttpResult?>(null) }
    val scope = rememberCoroutineScope()

    ScreenScaffold("HTTP 请求", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { m ->
                    FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                }
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("URL") }
            )
            OutlinedTextField(
                value = headers,
                onValueChange = { headers = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                label = { Text("请求头 (每行 Key: Value)") }
            )
            if (method in listOf("POST", "PUT", "PATCH", "DELETE")) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    label = { Text("请求体") }
                )
            }
            Button(
                onClick = {
                    sending = true
                    result = null
                    val parsedHeaders = headers.lineSequence()
                        .mapNotNull { line ->
                            val idx = line.indexOf(':')
                            if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
                        }.toList()
                    scope.launch {
                        result = repo.http(method, url.trim(), parsedHeaders, body.ifBlank { null })
                        sending = false
                    }
                },
                enabled = !sending && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (sending) "发送中…" else "发送请求")
            }
            result?.let { r ->
                SectionCard("响应") {
                    if (r.error != null) {
                        Text(r.error, color = MaterialTheme.colorScheme.error)
                    } else {
                        InfoRow("状态", "${r.status} ${r.message}")
                        InfoRow("耗时", "${r.timeMs} ms")
                        InfoRow("响应头", "${r.headers.size} 项")
                        InfoRow("大小", "${r.body.toByteArray().size} B")
                    }
                }
                if (r.headers.isNotEmpty()) {
                    SectionCard("响应头") {
                        r.headers.forEach { (k, v) -> InfoRow(k, v, mono = true) }
                    }
                }
                if (r.body.isNotBlank()) {
                    SectionCard("响应体") {
                        Text(
                            r.body,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
