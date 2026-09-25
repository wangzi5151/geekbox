@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.devtools

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard

@Composable
fun ConverterScreen(
    title: String,
    onBack: () -> Unit,
    inputHint: String,
    transform: (String) -> String,
    reverse: ((String) -> String)? = null
) {
    var input by remember { mutableStateOf("") }
    var forward by remember { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current

    val fn: (String) -> String = if (forward || reverse == null) transform else reverse
    val computed = remember(input, forward) {
        if (input.isEmpty()) null else runCatching { fn(input) }
    }
    val output = computed?.getOrNull().orEmpty()
    val error = computed?.exceptionOrNull()?.message

    ScreenScaffold(
        title,
        onBack,
        actions = {
            IconButton(onClick = { input = "" }) { Icon(Icons.Filled.Clear, contentDescription = "清空") }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (reverse != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = forward, onClick = { forward = true }, label = { Text("编码 / 转换") })
                    FilterChip(selected = !forward, onClick = { forward = false }, label = { Text("解码 / 还原") })
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                placeholder = { Text(inputHint) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) {
                            input = output
                        }
                    },
                    enabled = output.isNotEmpty()
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("交换")
                }
                OutlinedButton(
                    onClick = { if (output.isNotEmpty()) clipboard.setText(AnnotatedString(output)) },
                    enabled = output.isNotEmpty()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制")
                }
            }
            SectionCard("结果") {
                when {
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    output.isEmpty() -> Text("等待输入…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    else -> CodeBlock(output)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
