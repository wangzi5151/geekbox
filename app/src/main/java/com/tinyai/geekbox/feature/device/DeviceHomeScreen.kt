package com.tinyai.geekbox.feature.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.model.ToolItem
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ModuleHeader
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.components.ToolGrid
import com.tinyai.geekbox.core.ui.rememberAsync
import com.tinyai.geekbox.core.util.Formatters

@Composable
fun DeviceHomeScreen(onOpen: (ToolItem) -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    val state = rememberAsync { repo.overview() }

    ScreenScaffold("GeekBox", onBack = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModuleHeader(
                icon = Icons.Filled.PhoneAndroid,
                title = "设备",
                subtitle = "硬件信息 · 实时状态监控"
            )
            AsyncContent(state) { o ->
                SectionCard("设备概览") {
                    InfoRow("厂商", "${o.manufacturer} (${o.brand})")
                    InfoRow("型号", o.model, mono = true)
                    InfoRow("代号", "${o.device} / ${o.product}", mono = true)
                    InfoRow("Android", "${o.androidVersion} · API ${o.apiLevel}")
                    InfoRow("安全补丁", o.securityPatch)
                    InfoRow("内核", o.kernel, mono = true)
                    InfoRow("Build", o.buildId, mono = true)
                    InfoRow("运行时长", Formatters.uptime(o.uptimeMillis))
                    InfoRow("ABI", o.abis.joinToString(", "), mono = true)
                }
                Spacer(Modifier.height(4.dp))
                SectionCard("系统指纹") {
                    InfoRow("Fingerprint", o.fingerprint, mono = true)
                }
            }
            ToolGrid(DeviceTools.items) { onOpen(it) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
