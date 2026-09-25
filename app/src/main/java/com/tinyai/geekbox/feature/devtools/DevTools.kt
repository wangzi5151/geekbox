package com.tinyai.geekbox.feature.devtools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VpnKey
import com.tinyai.geekbox.core.model.ToolItem

object DevTools {
    val items = listOf(
        ToolItem("devtools/json", "JSON", "格式化 · 压缩 · 校验", Icons.Filled.DataObject),
        ToolItem("devtools/xml", "XML", "格式化与校验", Icons.Filled.DataObject),
        ToolItem("devtools/base64", "Base64", "文本编解码", Icons.Filled.Transform),
        ToolItem("devtools/hash", "Hash", "MD5/SHA-1/256/512", Icons.Filled.Fingerprint),
        ToolItem("devtools/hex", "HEX", "文本十六进制互转", Icons.Filled.Transform),
        ToolItem("devtools/regex", "正则", "匹配与分组测试", Icons.Filled.FindReplace),
        ToolItem("devtools/url", "URL", "编码与解码", Icons.Filled.Link),
        ToolItem("devtools/timestamp", "时间戳", "时间与 Epoch 互转", Icons.Filled.Schedule),
        ToolItem("devtools/uuid", "UUID", "批量生成 v4", Icons.Filled.Badge),
        ToolItem("devtools/qr", "二维码", "文本转二维码", Icons.Filled.QrCode2),
        ToolItem("devtools/barcode", "条形码", "Code128/EAN/Code39", Icons.Filled.QrCode),
        ToolItem("devtools/color", "颜色", "HEX/RGB/HSL 互转", Icons.Filled.Palette),
        ToolItem("devtools/text", "文本", "统计与批量转换", Icons.Filled.TextFields),
        ToolItem("devtools/diff", "文本差异", "逐行对比", Icons.Filled.CompareArrows),
        ToolItem("devtools/radix", "进制转换", "2/8/10/16 互转", Icons.Filled.Numbers),
        ToolItem("devtools/cron", "Cron 解析", "下次运行时间", Icons.Filled.EventRepeat),
        ToolItem("devtools/random", "随机生成", "密码 / PIN", Icons.Filled.Casino),
        ToolItem("devtools/subnet", "子网", "IP/CIDR 计算器", Icons.Filled.Calculate),
        ToolItem("devtools/jwt", "JWT", "解析 Header/Payload", Icons.Filled.VpnKey)
    )
}
