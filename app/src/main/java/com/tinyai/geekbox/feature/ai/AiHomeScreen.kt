@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.settings.AiOpenMode
import com.tinyai.geekbox.core.settings.AppSettings
import com.tinyai.geekbox.core.ui.components.ModuleHeader
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.SectionCard

private val palette = listOf(
    Color(0xFF22D3EE), Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFFBBF24),
    Color(0xFFFB7185), Color(0xFF60A5FA), Color(0xFFF472B6), Color(0xFF4ADE80),
    Color(0xFFF59E0B), Color(0xFF818CF8)
)

private fun colorFor(name: String): Color = palette[(name.hashCode().let { if (it < 0) -it else it }) % palette.size]

@Composable
fun AiHomeScreen(onOpen: (String, String) -> Unit) {
    val context = LocalContext.current
    var customUrl by remember { mutableStateOf("") }
    val systemMode = AppSettings.aiOpenMode == AiOpenMode.SYSTEM_BROWSER

    fun open(name: String, url: String) {
        if (systemMode) BrowserLauncher.openTab(context, url) else onOpen(name, url)
    }

    ScreenScaffold("GeekBox", onBack = null) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModuleHeader(
                icon = Icons.Filled.AutoAwesome,
                title = "AI 专区",
                subtitle = "聚合国内主流大模型"
            )

            SectionCard("打开方式") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = systemMode,
                        onClick = { AppSettings.setAiOpenMode(AiOpenMode.SYSTEM_BROWSER) },
                        label = { Text("系统浏览器内核 (推荐)") }
                    )
                    FilterChip(
                        selected = !systemMode,
                        onClick = { AppSettings.setAiOpenMode(AiOpenMode.WEBVIEW) },
                        label = { Text("应用内 WebView") }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (systemMode) "使用系统浏览器内核，以应用内标签页方式打开，兼容性最好（登录/输入/加载最稳）。"
                    else "完全内嵌的 WebView。若遇到输入顺序异常或站点打不开，请改用系统浏览器内核。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("自定义网址") {
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("输入任意 AI 网页地址") },
                    placeholder = { Text("https://chat.deepseek.com/") }
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { if (customUrl.isNotBlank()) open("自定义", customUrl.trim()) },
                    enabled = customUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("打开")
                }
            }

            Text("常用服务", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            AiServices.list.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { service ->
                        AiServiceCard(service, onClick = { open(service.name, service.url) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            SectionCard("说明") {
                Text(
                    "登录、验证码等流程由各站点自行处理；GeekBox 不读取、不保存你的账号或密码。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AiServiceCard(service: AiService, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(listOf(colorFor(service.name), colorFor(service.name).copy(alpha = 0.55f))),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    service.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0B0F14)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(service.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(service.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
