@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.network

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tinyai.geekbox.core.ui.components.EmptyState
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.StatBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun wifiPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun signalLevel(rssi: Int): Float = ((rssi + 100) / 60f).coerceIn(0f, 1f)

@Composable
fun WifiScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NetworkRepository(context) }
    val permissions = remember { wifiPermissions() }
    fun allGranted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(allGranted()) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<WifiScanEntry>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = allGranted()
    }

    fun scan() {
        if (!granted) { launcher.launch(permissions); return }
        loading = true
        message = null
        scope.launch {
            delay(500)
            val list = withContext(Dispatchers.IO) { runCatching { repo.scanWifi() }.getOrDefault(emptyList()) }
            results = list
            if (list.isEmpty()) message = "未获取到结果：请开启「位置服务 / 无线局域网」，稍后重试。"
            loading = false
        }
    }

    ScreenScaffold("WiFi 扫描", onBack, actions = {
        IconButton(onClick = { scan() }) { Icon(Icons.Filled.Refresh, contentDescription = "扫描") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!granted) {
                SectionCard("需要权限") {
                    Text(
                        "扫描附近 WiFi 需要定位/附近设备权限。GeekBox 仅在本机使用这些数据，不会上传。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { launcher.launch(permissions) }, modifier = Modifier.fillMaxWidth()) {
                        Text("授予权限")
                    }
                }
            } else {
                Button(onClick = { scan() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (loading) "扫描中…" else "扫描附近 WiFi")
                }
            }
            message?.let {
                SectionCard { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (results.isNotEmpty()) {
                Text("发现 ${results.size} 个网络", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.ssid, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                    Text("${item.rssi} dBm", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(6.dp))
                                StatBar(signalLevel(item.rssi))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${item.bssid} · ${item.frequency} MHz · ${if (item.capabilities.contains("WPA")) "加密" else "开放"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (granted && !loading) {
                EmptyState("点击「扫描附近 WiFi」开始")
            }
        }
    }
}
