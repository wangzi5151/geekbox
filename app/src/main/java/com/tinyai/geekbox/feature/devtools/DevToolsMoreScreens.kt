@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.devtools

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.tinyai.geekbox.core.ui.components.CodeBlock
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.util.CronUtils
import com.tinyai.geekbox.core.util.DiffUtils
import com.tinyai.geekbox.core.util.Formatters
import com.tinyai.geekbox.core.util.RadixUtils
import com.tinyai.geekbox.core.util.RandomUtils
import java.io.File

@Composable
fun RadixScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("255") }
    var fromRadix by remember { mutableStateOf(10) }
    val clipboard = LocalClipboardManager.current
    val targets = listOf(2 to "二进制", 8 to "八进制", 10 to "十进制", 16 to "十六进制")

    ScreenScaffold("进制转换", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("数值") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 8, 10, 16).forEach { r ->
                    FilterChip(selected = fromRadix == r, onClick = { fromRadix = r }, label = { Text("源 $r") })
                }
            }
            SectionCard("转换结果") {
                targets.forEach { (radix, label) ->
                    val result = remember(input, fromRadix, radix) {
                        runCatching { RadixUtils.convert(input, fromRadix, radix) }.getOrNull()
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        Text(
                            result ?: "无效",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = if (result != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        if (result != null) {
                            androidx.compose.material3.IconButton(onClick = { clipboard.setText(AnnotatedString(result)) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CronScreen(onBack: () -> Unit) {
    var expr by remember { mutableStateOf("*/5 * * * *") }
    val runs = remember(expr) { runCatching { CronUtils.nextRuns(expr, 5) } }
    val results = runs?.getOrNull().orEmpty()
    val error = runs?.exceptionOrNull()?.message

    ScreenScaffold("Cron 解析", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = expr, onValueChange = { expr = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cron 表达式 (分 时 日 月 周)") })
            SectionCard("预设") {
                listOf("*/5 * * * *", "0 * * * *", "0 0 * * *", "30 8 * * 1-5", "0 0 1 * *").forEach { preset ->
                    FilterChip(selected = expr == preset, onClick = { expr = preset }, label = { Text(preset) })
                    Spacer(Modifier.height(4.dp))
                }
            }
            SectionCard("接下来 5 次运行") {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else {
                    results.forEach { t ->
                        InfoRow(Formatters.time(t, "yyyy-MM-dd EEE HH:mm"), "", mono = true)
                    }
                }
            }
        }
    }
}

@Composable
fun DiffScreen(onBack: () -> Unit) {
    var left by remember { mutableStateOf("") }
    var right by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<DiffUtils.Line>>(emptyList()) }

    ScreenScaffold("文本差异", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = left, onValueChange = { left = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6, label = { Text("原始文本") })
            OutlinedTextField(value = right, onValueChange = { right = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6, label = { Text("对比文本") })
            Button(onClick = { lines = DiffUtils.diff(left, right) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("比较")
            }
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(lines) { line ->
                    val (prefix, color) = when (line.kind) {
                        DiffUtils.Kind.ADD -> "+ " to MaterialTheme.colorScheme.tertiary
                        DiffUtils.Kind.REMOVE -> "- " to MaterialTheme.colorScheme.error
                        DiffUtils.Kind.SAME -> "  " to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        prefix + line.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun RandomScreen(onBack: () -> Unit) {
    var length by remember { mutableStateOf(20) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    val values = remember { mutableStateListOf<String>() }
    val clipboard = LocalClipboardManager.current

    ScreenScaffold("随机生成器", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = length.toString(),
                onValueChange = { length = it.toIntOrNull()?.coerceIn(4, 128) ?: length },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("长度 (4-128)") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = upper, onClick = { upper = !upper }, label = { Text("大写") })
                FilterChip(selected = lower, onClick = { lower = !lower }, label = { Text("小写") })
                FilterChip(selected = digits, onClick = { digits = !digits }, label = { Text("数字") })
                FilterChip(selected = symbols, onClick = { symbols = !symbols }, label = { Text("符号") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    values.add(0, RandomUtils.password(length, upper, lower, digits, symbols))
                    if (values.size > 20) values.removeAt(values.lastIndex)
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("生成密码")
                }
                OutlinedButton(onClick = { values.add(0, RandomUtils.pin(length)) }, modifier = Modifier.weight(1f)) { Text("生成 PIN") }
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(values.joinToString("\n"))) }, enabled = values.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("复制全部")
            }
            SectionCard("结果") {
                if (values.isEmpty()) Text("点击生成…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else values.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(v, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        androidx.compose.material3.IconButton(onClick = { clipboard.setText(AnnotatedString(v)) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class BarcodeFormatOption(val label: String, val format: BarcodeFormat)

private val barcodeFormats = listOf(
    BarcodeFormatOption("Code128", BarcodeFormat.CODE_128),
    BarcodeFormatOption("EAN-13", BarcodeFormat.EAN_13),
    BarcodeFormatOption("Code39", BarcodeFormat.CODE_39),
    BarcodeFormatOption("QR", BarcodeFormat.QR_CODE)
)

@Composable
fun BarcodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("1234567890128") }
    var option by remember { mutableStateOf(barcodeFormats[1]) }
    val bitmap = remember(text, option) {
        if (text.isBlank()) null
        else runCatching {
            val matrix = MultiFormatWriter().encode(text, option.format, 800, 320)
            val pixels = IntArray(800 * 320)
            for (y in 0 until 320) for (x in 0 until 800) {
                pixels[y * 800 + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
            Bitmap.createBitmap(pixels, 800, 320, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    ScreenScaffold("条形码生成", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("内容") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                barcodeFormats.forEach { f ->
                    FilterChip(selected = option == f, onClick = { option = f }, label = { Text(f.label) })
                }
            }
            if (bitmap != null) {
                SectionCard("预览") {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "条形码",
                        modifier = Modifier.fillMaxWidth().aspectRatio(2.5f).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Button(
                    onClick = {
                        val target = File(context.getExternalFilesDir(null), "barcode_${option.label}.png")
                        val ok = runCatching { target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }.isSuccess
                        Toast.makeText(context, if (ok) "已保存: ${target.absolutePath}" else "保存失败", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("保存 PNG")
                }
            } else {
                SectionCard { Text("输入内容后自动生成（EAN-13 需 12/13 位数字）。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
