@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tinyai.geekbox.core.ui.components.ModuleHeader
import com.tinyai.geekbox.core.ui.components.ScreenScaffold
import com.tinyai.geekbox.core.ui.components.ToolGrid

@Composable
fun NetworkHomeScreen(onOpen: (String) -> Unit) {
    ScreenScaffold("GeekBox", onBack = null) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModuleHeader(
                icon = Icons.Filled.Lan,
                title = "网络",
                subtitle = "诊断 · 探测 · 请求调试"
            )
            ToolGrid(NetworkTools.items) { onOpen(it.route) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
