package com.tinyai.geekbox.core.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

data class Module(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
