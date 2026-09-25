package com.tinyai.geekbox.feature.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import com.tinyai.geekbox.core.model.ToolItem

object DeviceTools {
    val items = listOf(
        ToolItem("device/cpu", "CPU", "核心 · 频率 · 负载", Icons.Filled.Memory),
        ToolItem("device/gpu", "GPU", "图形处理器 · Best-effort", Icons.Filled.DeveloperBoard),
        ToolItem("device/memory", "内存", "物理内存 · 交换分区", Icons.Filled.Storage),
        ToolItem("device/battery", "电池", "电量 · 温度 · 健康", Icons.Filled.BatteryFull),
        ToolItem("device/storage", "存储", "内部与外部空间", Icons.Filled.SdCard),
        ToolItem("device/screen", "屏幕", "分辨率 · 刷新率 · HDR", Icons.Filled.Smartphone),
        ToolItem("device/camera", "摄像头", "镜头 · 分辨率 · 级别", Icons.Filled.PhotoCamera),
        ToolItem("device/sensors", "传感器", "实时传感器列表", Icons.Filled.Sensors),
        ToolItem("device/thermal", "温度", "热区温度 · Root 增强", Icons.Filled.Thermostat),
        ToolItem("device/flashlight", "手电筒", "闪光灯开关", Icons.Filled.FlashlightOn),
        ToolItem("device/volume", "音量", "各音频流调节", Icons.Filled.VolumeUp),
        ToolItem("device/screentest", "屏幕测试", "纯色检查坏点", Icons.Filled.ColorLens),
        ToolItem("device/report", "设备报告", "汇总并复制", Icons.Filled.Summarize)
    )
}
