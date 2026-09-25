package com.tinyai.geekbox.feature.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import com.tinyai.geekbox.core.model.ToolItem

object NetworkTools {
    val items = listOf(
        ToolItem("network/info", "网络信息", "接口 · WiFi · DNS", Icons.Filled.Wifi),
        ToolItem("network/speed", "网络速率", "实时上下行 · 曲线", Icons.Filled.Speed),
        ToolItem("network/connections", "网络连接", "TCP/UDP 连接表", Icons.Filled.Cable),
        ToolItem("network/listen", "监听端口", "本机监听/绑定端口", Icons.Filled.SettingsEthernet),
        ToolItem("network/traffic", "应用流量", "各应用流量统计", Icons.Filled.DataUsage),
        ToolItem("network/wifi", "WiFi 扫描", "附近热点与信号", Icons.Filled.WifiFind),
        ToolItem("network/dns", "DNS 查询", "域名解析记录", Icons.Filled.Dns),
        ToolItem("network/dnsbench", "DNS 基准", "多 DNS 解析测速", Icons.Filled.Bolt),
        ToolItem("network/ptr", "反向解析", "IP → 主机名", Icons.Filled.ManageSearch),
        ToolItem("network/ping", "Ping", "连通性与延迟", Icons.Filled.NetworkCheck),
        ToolItem("network/traceroute", "Traceroute", "路由跳数追踪", Icons.Filled.Route),
        ToolItem("network/redirect", "重定向追踪", "HTTP 跳转链", Icons.Filled.CallSplit),
        ToolItem("network/ntp", "NTP 校时", "网络时间与偏差", Icons.Filled.AccessTime),
        ToolItem("network/wol", "Wake-on-LAN", "局域网唤醒", Icons.Filled.PowerSettingsNew),
        ToolItem("network/http", "HTTP", "请求调试与响应", Icons.Filled.Http),
        ToolItem("network/cert", "TLS 证书", "证书链与到期时间", Icons.Filled.VerifiedUser),
        ToolItem("network/whois", "WHOIS", "域名/IP 注册信息", Icons.Filled.TravelExplore),
        ToolItem("network/download", "下载器", "下载文件并测速", Icons.Filled.Download),
        ToolItem("network/portscan", "端口扫描", "TCP 开放端口", Icons.Filled.Radar),
        ToolItem("network/lanscan", "局域网扫描", "发现同网段主机", Icons.Filled.Devices)
    )
}
