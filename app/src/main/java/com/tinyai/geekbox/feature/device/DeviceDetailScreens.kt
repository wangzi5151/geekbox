@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.Sparkline
import com.tinyai.geekbox.core.ui.components.StatBar
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.ui.rememberPollingAsync
import com.tinyai.geekbox.core.util.Formatters

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
    }
}

@Composable
fun CpuDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var auto by remember { mutableStateOf(true) }
    var manual by remember { mutableIntStateOf(0) }
    val state = rememberPollingAsync(1500, auto, manual) { repo.cpu() }
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(state) {
        (state as? com.tinyai.geekbox.core.ui.Async.Success)?.let { s ->
            val ratios = s.value.cores.mapNotNull { c ->
                val cur = c.curKhz
                val max = c.maxKhz
                if (cur != null && max != null && max > 0) cur.toFloat() / max.toFloat() else null
            }
            if (ratios.isNotEmpty()) {
                history.add(ratios.average().toFloat())
                if (history.size > 60) history.removeAt(0)
            }
        }
    }

    ScreenScaffold("CPU", onBack, actions = { RefreshButton { manual++ } }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = auto,
                onClick = { auto = !auto },
                label = { Text(if (auto) "自动刷新：开 (1.5s)" else "自动刷新：关") }
            )
            AsyncContent(state) { cpu ->
                val avgRatio = if (history.isEmpty()) 0f else history.last()
                SectionCard("实时负载曲线") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("平均频率占用", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${(avgRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Sparkline(
                        values = history.toList(),
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        maxValue = 1f
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("最近 ${history.size} 个采样点", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionCard("概览") {
                    InfoRow("逻辑核心", cpu.cores.size.toString())
                    InfoRow("硬件", cpu.hardware, mono = true)
                    InfoRow("负载 (1/5/15m)", cpu.loadAvg, mono = true)
                }
                SectionCard("核心频率") {
                    cpu.cores.forEach { core ->
                        val cur = core.curKhz
                        val max = core.maxKhz
                        val ratio = if (cur != null && max != null && max > 0) {
                            cur.toFloat() / max.toFloat()
                        } else 0f
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("核心 ${core.index}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Text(
                                    "${Formatters.hz(core.curKhz ?: 0)} / ${Formatters.hz(core.maxKhz ?: 0)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            StatBar(ratio)
                            core.governor?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("调速器: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var auto by remember { mutableStateOf(true) }
    var manual by remember { mutableIntStateOf(0) }
    val state = rememberPollingAsync(2000, auto, manual) { repo.memory() }
    val history = remember { mutableStateListOf<Float>() }
    LaunchedEffect(state) {
        (state as? com.tinyai.geekbox.core.ui.Async.Success)?.let { s ->
            history.add(Formatters.percent(s.value.used.toDouble(), s.value.total.toDouble()))
            if (history.size > 60) history.removeAt(0)
        }
    }
    ScreenScaffold("内存", onBack, actions = { RefreshButton { manual++ } }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = auto,
                onClick = { auto = !auto },
                label = { Text(if (auto) "自动刷新：开 (2s)" else "自动刷新：关") }
            )
            AsyncContent(state) { m ->
                val usedRatio = Formatters.percent(m.used.toDouble(), m.total.toDouble())
                SectionCard("物理内存") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("已用 ${Formatters.bytes(m.used)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(Formatters.percentText(m.used.toDouble(), m.total.toDouble()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    StatBar(usedRatio)
                    if (history.size >= 2) {
                        Spacer(Modifier.height(12.dp))
                        Sparkline(
                            values = history.toList(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            maxValue = 1f
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("最近 ${history.size} 个采样点", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("总计", Formatters.bytes(m.total))
                    InfoRow("可用", Formatters.bytes(m.available))
                    InfoRow("低内存状态", if (m.lowMemory) "是" else "否", valueColor = if (m.lowMemory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    InfoRow("阈值", Formatters.bytes(m.threshold))
                }
                if (m.swapTotal > 0) {
                    SectionCard("交换分区") {
                        val swapUsed = (m.swapTotal - m.swapFree).coerceAtLeast(0)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("已用 ${Formatters.bytes(swapUsed)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(Formatters.percentText(swapUsed.toDouble(), m.swapTotal.toDouble()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        StatBar(Formatters.percent(swapUsed.toDouble(), m.swapTotal.toDouble()), color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(12.dp))
                        InfoRow("总计", Formatters.bytes(m.swapTotal))
                        InfoRow("空闲", Formatters.bytes(m.swapFree))
                    }
                }
                if (m.details.isNotEmpty()) {
                    SectionCard("内存明细 (/proc/meminfo)") {
                        m.details.forEach { InfoRow(it.label, Formatters.bytes(it.bytes)) }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var auto by remember { mutableStateOf(true) }
    var manual by remember { mutableIntStateOf(0) }
    val state = rememberPollingAsync(3000, auto, manual) { repo.battery() }
    val tempHistory = remember { mutableStateListOf<Float>() }
    LaunchedEffect(state) {
        (state as? com.tinyai.geekbox.core.ui.Async.Success)?.let { s ->
            tempHistory.add(s.value.temperatureC)
            if (tempHistory.size > 40) tempHistory.removeAt(0)
        }
    }
    ScreenScaffold("电池", onBack, actions = { RefreshButton { manual++ } }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = auto,
                onClick = { auto = !auto },
                label = { Text(if (auto) "自动刷新：开 (3s)" else "自动刷新：关") }
            )
            AsyncContent(state) { b ->
                SectionCard("电量") {
                    Text(
                        if (b.level >= 0) "${b.level}%" else "未知",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    StatBar(if (b.level >= 0) b.level / 100f else 0f)
                }
                if (tempHistory.size >= 2) {
                    SectionCard("温度曲线") {
                        Sparkline(
                            values = tempHistory.toList(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            color = MaterialTheme.colorScheme.error,
                            minValue = tempHistory.min(),
                            maxValue = maxOf(tempHistory.max(), tempHistory.min() + 1f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "最低 %.1f°C · 最高 %.1f°C".format(tempHistory.min(), tempHistory.max()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SectionCard("状态") {
                    InfoRow("充电状态", b.status)
                    InfoRow("电源", b.plugged)
                    InfoRow("健康", b.health)
                    InfoRow("温度", "%.1f °C".format(b.temperatureC))
                    InfoRow("电压", "${b.voltageMv} mV")
                    InfoRow("技术", b.technology, mono = true)
                    InfoRow("存在", if (b.present) "是" else "否")
                    if (b.capacityMah > 0) InfoRow("设计容量", "${b.capacityMah} mAh")
                }
            }
        }
    }
}

@Composable
fun StorageDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    ScreenScaffold("存储", onBack, actions = { RefreshButton { refresh++ } }) { padding ->
        val state = rememberAsync(refresh) { repo.storage() }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { list ->
                list.forEach { s ->
                    val used = (s.total - s.free).coerceAtLeast(0)
                    SectionCard(s.label) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("已用 ${Formatters.bytes(used)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(Formatters.percentText(used.toDouble(), s.total.toDouble()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(10.dp))
                        StatBar(Formatters.percent(used.toDouble(), s.total.toDouble()))
                        Spacer(Modifier.height(12.dp))
                        InfoRow("总计", Formatters.bytes(s.total))
                        InfoRow("可用", Formatters.bytes(s.free))
                        InfoRow("路径", s.path, mono = true)
                    }
                }
            }
        }
    }
}

@Composable
fun SensorsScreen(onBack: () -> Unit, onOpenLive: (Int) -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    val state = rememberAsync { repo.sensors() }
    ScreenScaffold("传感器", onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val s = state) {
                is com.tinyai.geekbox.core.ui.Async.Success -> {
                    items(s.value) { sensor ->
                        androidx.compose.material3.Card(
                            onClick = { onOpenLive(sensor.type) },
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(sensor.typeName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text(sensor.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "vendor=${sensor.vendor} · range=${sensor.maxRange} · res=${sensor.resolution} · power=%.3fmA".format(sensor.power),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (s.value.isEmpty()) item { Text("未发现传感器", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                is com.tinyai.geekbox.core.ui.Async.Loading -> item { com.tinyai.geekbox.core.ui.components.LoadingBox() }
                is com.tinyai.geekbox.core.ui.Async.Error -> item { com.tinyai.geekbox.core.ui.components.ErrorBox(s.message) }
            }
        }
    }
}

@Composable
fun SensorLiveScreen(onBack: () -> Unit, sensorType: Int) {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember(sensorType) { sm.getSensorList(Sensor.TYPE_ALL).firstOrNull { it.type == sensorType } }
    var values by remember { mutableStateOf(FloatArray(0)) }
    var accuracy by remember { mutableIntStateOf(-1) }
    var label by remember { mutableStateOf("-") }

    DisposableEffect(sensorType) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                values = event.values.copyOf()
                label = "$sensorType"
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) { accuracy = a }
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    ScreenScaffold(sensor?.let { "实时: ${it.name}" } ?: "实时传感器", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("数值") {
                if (values.isEmpty()) {
                    Text("等待数据…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    values.forEachIndexed { i, v ->
                        InfoRow("轴 $i", "%.5f".format(v), mono = true)
                    }
                }
            }
            SectionCard("信息") {
                InfoRow("名称", sensor?.name ?: "-")
                InfoRow("厂商", sensor?.vendor ?: "-")
                InfoRow("精度", accuracy.toString())
            }
        }
    }
}

@Composable
fun ThermalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    ScreenScaffold("温度", onBack, actions = { RefreshButton { refresh++ } }) { padding ->
        val state = rememberAsync(refresh) { repo.thermal() }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { zones ->
                if (zones.isEmpty()) {
                    SectionCard {
                        Text("未读取到热区数据。部分设备需 Root 才能访问 /sys/class/thermal。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SectionCard("热区") {
                        zones.forEach { z ->
                            val color = when {
                                z.tempC >= 60 -> MaterialTheme.colorScheme.error
                                z.tempC >= 45 -> Color(0xFFFBBF24)
                                else -> MaterialTheme.colorScheme.tertiary
                            }
                            InfoRow(z.name, "%.1f °C".format(z.tempC), mono = true, valueColor = color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GpuScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    ScreenScaffold("GPU", onBack, actions = { RefreshButton { refresh++ } }) { padding ->
        val state = rememberAsync(refresh) { repo.gpu() }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { g ->
                SectionCard("图形处理器") {
                    InfoRow("型号", g.model ?: "未知")
                    g.curKhz?.let { InfoRow("当前频率", Formatters.hz(it)) }
                    g.maxKhz?.let { InfoRow("最大频率", Formatters.hz(it)) }
                    g.minKhz?.let { InfoRow("最小频率", Formatters.hz(it)) }
                    if (g.model == null && g.curKhz == null) {
                        Text(
                            "部分设备对外隐藏 GPU 节点。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (g.extras.isNotEmpty()) {
                    SectionCard("更多") {
                        g.extras.forEach { (k, v) -> InfoRow(k, v, mono = true) }
                    }
                }
                g.note?.let {
                    SectionCard { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
fun ScreenInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    var manual by remember { mutableIntStateOf(0) }
    ScreenScaffold("屏幕", onBack, actions = { RefreshButton { manual++ } }) { padding ->
        val state = rememberAsync(manual) { repo.screen() }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { s ->
                SectionCard("分辨率") {
                    InfoRow("像素", "${s.widthPx} × ${s.heightPx}", mono = true)
                    InfoRow("密度", "${s.densityDpi} dpi (${s.sizeClass})")
                    InfoRow("比例", "%.2f".format(s.density), mono = true)
                    InfoRow("X/Y dpi", "%.0f / %.0f".format(s.xdpi, s.ydpi), mono = true)
                }
                SectionCard("刷新率") {
                    InfoRow("当前", "%.1f Hz".format(s.refreshRate))
                    if (s.refreshRates.isNotEmpty()) {
                        InfoRow("支持", s.refreshRates.joinToString(", ") { "%.0f".format(it) } + " Hz", mono = true)
                    }
                }
                SectionCard("HDR") {
                    if (s.hdr.isEmpty()) Text("未报告 HDR 支持", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else s.hdr.forEach { InfoRow(it, "支持", valueColor = MaterialTheme.colorScheme.tertiary) }
                }
            }
        }
    }
}
