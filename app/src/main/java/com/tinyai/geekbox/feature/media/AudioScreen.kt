@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.media

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AudioRepository(context) }
    var sourcePath by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf<String?>(null) }
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    var decoding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "audio"
                    val target = File(context.cacheDir, "audio_in_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("无法读取文件")
                    target.absolutePath to name
                }
            }.onSuccess { (path, name) ->
                sourcePath = path
                displayName = name
                meta = runCatching { repo.metadata(path) }.getOrNull()
            }.onFailure { error = it.message ?: "读取失败" }
        }
    }

    ScreenScaffold("音频解码", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("选择音频文件")
            }
            error?.let { SectionCard { Text(it, color = MaterialTheme.colorScheme.error) } }

            meta?.let { m ->
                SectionCard("文件信息") {
                    InfoRow("名称", displayName ?: "-")
                    InfoRow("标题", m.title ?: "-")
                    InfoRow("艺术家", m.artist ?: "-")
                    InfoRow("专辑", m.album ?: "-")
                    InfoRow("时长", Formatters.uptime(m.durationMs))
                    if (m.bitrate > 0) InfoRow("码率", "${m.bitrate / 1000} kbps")
                    InfoRow("编码", m.mime ?: "-", mono = true)
                    if (m.sampleRate > 0) InfoRow("采样率", "${m.sampleRate} Hz")
                    if (m.channels > 0) InfoRow("声道", m.channels.toString())
                }
                Button(
                    onClick = {
                        val path = sourcePath ?: return@Button
                        decoding = true
                        error = null
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val dir = File(context.getExternalFilesDir(null), "decoded").apply { mkdirs() }
                                    val base = (displayName ?: "audio").substringBeforeLast('.')
                                    val target = File(dir, "$base.wav")
                                    repo.decodeToWav(path, target)
                                }
                            }.onSuccess { r ->
                                Toast.makeText(
                                    context,
                                    "已导出: ${r.targetPath}  (${Formatters.bytes(r.pcmBytes)})",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { error = "解码失败: ${it.message}" }
                            decoding = false
                        }
                    },
                    enabled = !decoding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Transform, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (decoding) "解码中…" else "解码为 WAV (PCM)")
                }
            }

            SectionCard("说明") {
                Text(
                    "将 MediaCodec 支持的常见音频（MP3/AAC/FLAC/OGG/…）解码为无损 WAV(PCM)，保存在应用外部目录 decoded/。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "仅支持未加密的公开音频格式。受保护/加密的专有格式（如某些音乐客户端的加密缓存）不在此工具支持范围内。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
