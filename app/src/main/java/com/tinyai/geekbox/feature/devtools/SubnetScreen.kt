@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.devtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.util.IpUtils

@Composable
fun SubnetScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("192.168.1.10/24") }
    val info = remember(input) { IpUtils.parseCidr(input) }
    val clipboard = LocalClipboardManager.current

    ScreenScaffold("子网计算器", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("IPv4 或 CIDR") },
                placeholder = { Text("192.168.1.10/24") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8, 16, 24, 25, 30).forEach { p ->
                    FilterChip(
                        selected = info?.prefix == p,
                        onClick = {
                            val ip = input.substringBefore("/").ifBlank { "192.168.1.1" }
                            input = "$ip/$p"
                        },
                        label = { Text("/$p") }
                    )
                }
            }
            if (info != null) {
                SectionCard("结果") {
                    InfoRow("网络地址", info.network, mono = true, valueColor = MaterialTheme.colorScheme.primary)
                    InfoRow("广播地址", info.broadcast, mono = true)
                    InfoRow("可用范围", "${info.firstHost} - ${info.lastHost}", mono = true)
                    InfoRow("子网掩码", info.netmask, mono = true)
                    InfoRow("反掩码", info.wildcard, mono = true)
                    InfoRow("可用主机数", info.hostCount.toString())
                    InfoRow("私有地址", if (info.isPrivate) "是" else "否")
                }
                FilterChip(
                    selected = false,
                    onClick = { clipboard.setText(AnnotatedString("${info.network}/${info.prefix}")) },
                    label = { Text("复制网络段") }
                )
            } else {
                SectionCard {
                    Text("输入形如 192.168.1.10/24 的地址。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
