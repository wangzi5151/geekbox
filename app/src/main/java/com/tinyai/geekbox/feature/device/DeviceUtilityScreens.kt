@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.device

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters

@Composable
fun FlashlightScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember {
        runCatching {
            manager?.cameraIdList?.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }

    fun setTorch(value: Boolean) {
        val id = cameraId ?: return
        runCatching { manager?.setTorchMode(id, value) }
        on = value
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { manager?.setTorchMode(cameraId ?: "", false) } }
    }

    ScreenScaffold("手电筒", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraId == null) {
                SectionCard { Text("未检测到可用闪光灯。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { setTorch(!on) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FlashlightOn,
                        contentDescription = null,
                        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Button(onClick = { setTorch(!on) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (on) "关闭" else "开启")
                }
            }
        }
    }
}

private data class VolumeStream(val stream: Int, val label: String)

private val volumeStreams = listOf(
    VolumeStream(AudioManager.STREAM_MUSIC, "媒体"),
    VolumeStream(AudioManager.STREAM_RING, "铃声"),
    VolumeStream(AudioManager.STREAM_ALARM, "闹钟"),
    VolumeStream(AudioManager.STREAM_NOTIFICATION, "通知"),
    VolumeStream(AudioManager.STREAM_SYSTEM, "系统"),
    VolumeStream(AudioManager.STREAM_VOICE_CALL, "通话")
)

@Composable
fun VolumeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var tick by remember { mutableStateOf(0) }

    ScreenScaffold("音量控制", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            volumeStreams.forEach { vs ->
                val max = audio?.getStreamMaxVolume(vs.stream) ?: 0
                val current = audio?.getStreamVolume(vs.stream) ?: 0
                SectionCard(vs.label) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("$current / $max", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = current.toFloat(),
                        onValueChange = { v ->
                            audio?.setStreamVolume(vs.stream, v.toInt(), 0)
                            tick++
                        },
                        valueRange = 0f..(max.coerceAtLeast(1)).toFloat(),
                        steps = (max - 1).coerceAtLeast(0)
                    )
                }
            }
            SectionCard {
                Text("直接调节各音频流音量（${tick}）。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val testColors = listOf(
    "红" to Color(0xFFFF0000),
    "绿" to Color(0xFF00FF00),
    "蓝" to Color(0xFF0000FF),
    "白" to Color(0xFFFFFFFF),
    "黑" to Color(0xFF000000),
    "灰" to Color(0xFF808080)
)

@Composable
fun ScreenTestScreen(onBack: () -> Unit) {
    var color by remember { mutableStateOf(testColors.first().second) }

    ScreenScaffold("屏幕测试", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(color, RoundedCornerShape(12.dp))
                    .clickable { }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                testColors.forEach { (name, c) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.6f)
                                .background(c, RoundedCornerShape(8.dp))
                                .clickable { color = c }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("全屏纯色用于检查坏点/亮度均匀性。点击方块切换颜色。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DeviceReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    val clipboard = LocalClipboardManager.current
    val state = rememberAsync {
        val o = repo.overview()
        val cpu = repo.cpu()
        val mem = repo.memory()
        val batt = repo.battery()
        val storage = repo.storage()
        val screen = repo.screen()
        buildString {
            appendLine("===== GeekBox 设备报告 =====")
            appendLine("时间: ${Formatters.time(System.currentTimeMillis())}")
            appendLine()
            appendLine("[设备]")
            appendLine("厂商: ${o.manufacturer} (${o.brand})")
            appendLine("型号: ${o.model}")
            appendLine("代号: ${o.device} / ${o.product}")
            appendLine("Android: ${o.androidVersion} (API ${o.apiLevel})")
            appendLine("安全补丁: ${o.securityPatch}")
            appendLine("内核: ${o.kernel}")
            appendLine("Build: ${o.buildId}")
            appendLine("ABI: ${o.abis.joinToString(", ")}")
            appendLine("运行时长: ${Formatters.uptime(o.uptimeMillis)}")
            appendLine("指纹: ${o.fingerprint}")
            appendLine()
            appendLine("[CPU]")
            appendLine("核心: ${cpu.cores.size}, 硬件: ${cpu.hardware}")
            appendLine("负载: ${cpu.loadAvg}")
            cpu.cores.forEach { appendLine("  核${it.index}: ${Formatters.hz(it.curKhz ?: 0)} / ${Formatters.hz(it.maxKhz ?: 0)}") }
            appendLine()
            appendLine("[内存]")
            appendLine("总计: ${Formatters.bytes(mem.total)}, 可用: ${Formatters.bytes(mem.available)}")
            appendLine("Swap: ${Formatters.bytes(mem.swapTotal)} (空闲 ${Formatters.bytes(mem.swapFree)})")
            appendLine()
            appendLine("[电池]")
            appendLine("电量: ${batt.level}%, 状态: ${batt.status}, 健康: ${batt.health}")
            appendLine("温度: ${batt.temperatureC}°C, 电压: ${batt.voltageMv}mV, 技术: ${batt.technology}")
            appendLine()
            appendLine("[存储]")
            storage.forEach { appendLine("${it.label}: 可用 ${Formatters.bytes(it.free)} / ${Formatters.bytes(it.total)}") }
            appendLine()
            appendLine("[屏幕]")
            appendLine("分辨率: ${screen.widthPx}x${screen.heightPx}, DPI: ${screen.densityDpi}, 刷新率: ${screen.refreshRate}Hz")
        }
    }

    ScreenScaffold("设备报告", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncContent(state) { report ->
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(report)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制报告")
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(report, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
