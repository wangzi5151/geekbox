@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
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
fun VideoInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AudioRepository(context) }
    var displayName by remember { mutableStateOf<String?>(null) }
    var meta by remember { mutableStateOf<VideoMeta?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "video"
                    val target = File(context.cacheDir, "video_in_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("无法读取文件")
                    name to repo.videoMeta(target.absolutePath)
                }
            }.onSuccess { (name, m) -> displayName = name; meta = m }
                .onFailure { error = it.message ?: "读取失败" }
        }
    }

    ScreenScaffold("视频信息", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("选择视频文件")
            }
            error?.let { SectionCard { Text(it, color = MaterialTheme.colorScheme.error) } }
            meta?.let { m ->
                SectionCard("视频信息") {
                    InfoRow("名称", displayName ?: "-")
                    InfoRow("分辨率", "${m.width} × ${m.height}", mono = true)
                    InfoRow("时长", Formatters.uptime(m.durationMs))
                    if (m.bitrate > 0) InfoRow("码率", "${m.bitrate / 1000} kbps")
                    InfoRow("旋转", "${m.rotation}°")
                    m.frameRate?.let { InfoRow("帧率", "$it fps") }
                    InfoRow("编码", m.mime ?: "-", mono = true)
                }
            }
            SectionCard("说明") {
                Text(
                    "读取本地视频的容器/编码信息，不上传任何数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
