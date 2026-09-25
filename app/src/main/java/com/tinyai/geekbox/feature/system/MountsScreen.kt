@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.ui.Async
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.rememberAsync

@Composable
fun MountsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SystemRepository(context) }
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    val state = rememberAsync(refresh) { repo.mounts() }

    ScreenScaffold("挂载 / 分区", onBack, actions = {
        IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, contentDescription = "刷新") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "执行权限：${if (ShizukuBridge.isAuthorized()) "Shizuku (shell)" else "本地"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("搜索挂载点 / 设备") })
            when (val s = state) {
                is Async.Loading -> com.tinyai.geekbox.core.ui.components.LoadingBox()
                is Async.Error -> com.tinyai.geekbox.core.ui.components.ErrorBox(s.message)
                is Async.Success -> {
                    val list = s.value.filter { query.isBlank() || it.mountPoint.contains(query, true) || it.device.contains(query, true) || it.fsType.contains(query, true) }
                    Text("共 ${list.size} 项", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(list) { m ->
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(m.mountPoint, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(m.device, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                                    Text("${m.fsType} · ${m.options.take(60)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
