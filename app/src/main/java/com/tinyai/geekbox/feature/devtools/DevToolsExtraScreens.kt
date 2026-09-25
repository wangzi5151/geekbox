@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.devtools

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.util.ColorUtils

private val swatches = listOf(
    "#22D3EE", "#A78BFA", "#34D399", "#FBBF24", "#FB7185",
    "#0B0F14", "#FFFFFF", "#3B82F6", "#F97316", "#10B981"
)

@Composable
fun ColorScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("#22D3EE") }
    val rgb = remember(input) { ColorUtils.parseHex(input) }
    val clipboard = LocalClipboardManager.current

    ScreenScaffold("颜色转换", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("HEX 颜色") },
                placeholder = { Text("#22D3EE / 22D3EE / #FFF") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                swatches.forEach { hex ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(
                                ColorUtils.parseHex(hex)?.let { Color(it.r, it.g, it.b) } ?: Color.Gray,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { input = hex }
                    )
                }
            }
            if (rgb != null) {
                SectionCard("预览") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(rgb.r, rgb.g, rgb.b), RoundedCornerShape(12.dp))
                    )
                }
                SectionCard("表示法") {
                    val (h, s, l) = ColorUtils.toHsl(rgb)
                    InfoRow("HEX", ColorUtils.toHex(rgb), mono = true)
                    InfoRow("RGB", "rgb(${rgb.r}, ${rgb.g}, ${rgb.b})", mono = true)
                    InfoRow("HSL", "hsl(%.0f, %.0f%%, %.0f%%)".format(h, s, l), mono = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(
                                AnnotatedString(
                                    "${ColorUtils.toHex(rgb)}\nrgb(${rgb.r}, ${rgb.g}, ${rgb.b})\nhsl(%.0f, %.0f%%, %.0f%%)".format(h, s, l)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("复制全部") }
                }
            } else {
                SectionCard { Text("无效的颜色值。", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun transformAll(input: String): Map<String, String> = linkedMapOf(
    "大写" to input.uppercase(),
    "小写" to input.lowercase(),
    "反转" to input.reversed(),
    "去除首尾空白" to input.trim(),
    "去除空行" to input.lines().filter { it.isNotBlank() }.joinToString("\n"),
    "行去重" to input.lines().distinct().joinToString("\n"),
    "行升序" to input.lines().sorted().joinToString("\n"),
    "行降序" to input.lines().sortedDescending().joinToString("\n")
)

@Composable
fun TextToolScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    ScreenScaffold("文本工具", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                label = { Text("输入文本") }
            )
            val words = input.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            SectionCard("统计") {
                InfoRow("字符数", input.length.toString())
                InfoRow("不含空白", input.count { !it.isWhitespace() }.toString())
                InfoRow("单词数", words.toString())
                InfoRow("行数", input.lines().size.toString())
                InfoRow("字节(UTF-8)", input.toByteArray().size.toString())
            }
            SectionCard("转换") {
                val transforms = transformAll(input)
                transforms.keys.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            OutlinedButton(
                                onClick = { output = transforms.getValue(key) },
                                enabled = input.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) { Text(key) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            SectionCard("结果") {
                if (output.isEmpty()) {
                    Text("选择上方操作后在此显示。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CodeBlock(output)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { clipboard.setText(AnnotatedString(output)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("复制结果")
                    }
                }
            }
        }
    }
}
