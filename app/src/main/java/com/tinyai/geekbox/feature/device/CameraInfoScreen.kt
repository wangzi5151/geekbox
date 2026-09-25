@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.AsyncContent
import com.tinyai.geekbox.core.ui.components.InfoRow
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard
import com.tinyai.geekbox.core.ui.rememberAsync

@Composable
fun CameraInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { DeviceRepository(context) }
    val state = rememberAsync { repo.cameras() }

    ScreenScaffold("摄像头", onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncContent(state) { cameras ->
                if (cameras.isEmpty()) {
                    SectionCard { androidx.compose.material3.Text("未读取到摄像头信息。", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                cameras.forEach { cam ->
                    SectionCard("摄像头 ${cam.id} (${cam.facing})") {
                        InfoRow("硬件级别", cam.hardwareLevel)
                        InfoRow("传感器方向", "${cam.orientation}°")
                        InfoRow("闪光灯", if (cam.flashAvailable) "有" else "无")
                        if (cam.maxWidth > 0) InfoRow("最大分辨率", "${cam.maxWidth} × ${cam.maxHeight}", mono = true)
                        if (cam.focalLengths.isNotEmpty()) {
                            InfoRow("焦距 (mm)", cam.focalLengths.joinToString(", ") { "%.2f".format(it) }, mono = true)
                        }
                        InfoRow("对焦模式数", cam.afModes.toString())
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
