package com.tinyai.geekbox.feature.system

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import com.tinyai.geekbox.core.model.ToolItem

object SystemTools {
    val items = listOf(
        ToolItem("system/shizuku", "Shizuku", "免 Root 系统级能力", Icons.Filled.Extension),
        ToolItem("system/shell", "Shell", "执行系统命令", Icons.Filled.Terminal),
        ToolItem("system/logcat", "Logcat", "日志抓取", Icons.Filled.Terminal),
        ToolItem("system/processes", "进程管理", "查看与结束进程", Icons.Filled.AccountTree),
        ToolItem("system/appmanager", "应用管理", "冻结/停用/清数据", Icons.Filled.ManageAccounts),
        ToolItem("system/appstorage", "应用存储", "各应用占用空间", Icons.Filled.Storage),
        ToolItem("system/dumpsys", "dumpsys", "系统服务信息", Icons.Filled.Description),
        ToolItem("system/mounts", "挂载/分区", "分区与挂载点", Icons.Filled.Folder),
        ToolItem("system/intent", "Intent / URI", "链接解析与启动", Icons.AutoMirrored.Filled.Launch),
        ToolItem("system/root", "Root 能力", "权限与安全状态", Icons.Filled.AdminPanelSettings),
        ToolItem("system/props", "系统属性", "getprop 全部属性", Icons.Filled.Api),
        ToolItem("system/shortcuts", "系统设置", "快捷进入常用设置", Icons.Filled.Tune),
        ToolItem("system/about", "关于", "版本 · 主题 · 隐私", Icons.Filled.Info)
    )
}
