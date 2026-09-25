package com.tinyai.geekbox.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tinyai.geekbox.feature.ai.AiHomeScreen
import com.tinyai.geekbox.feature.ai.AiWebScreen
import com.tinyai.geekbox.feature.apps.ApkInspectScreen
import com.tinyai.geekbox.feature.apps.AppDetailScreen
import com.tinyai.geekbox.feature.apps.AppsHomeScreen
import com.tinyai.geekbox.feature.device.BatteryDetailScreen
import com.tinyai.geekbox.feature.device.CameraInfoScreen
import com.tinyai.geekbox.feature.device.CpuDetailScreen
import com.tinyai.geekbox.feature.device.DeviceHomeScreen
import com.tinyai.geekbox.feature.device.DeviceReportScreen
import com.tinyai.geekbox.feature.device.FlashlightScreen
import com.tinyai.geekbox.feature.device.GpuScreen
import com.tinyai.geekbox.feature.device.MemoryDetailScreen
import com.tinyai.geekbox.feature.device.ScreenInfoScreen
import com.tinyai.geekbox.feature.device.ScreenTestScreen
import com.tinyai.geekbox.feature.device.SensorLiveScreen
import com.tinyai.geekbox.feature.device.SensorsScreen
import com.tinyai.geekbox.feature.device.StorageDetailScreen
import com.tinyai.geekbox.feature.device.ThermalScreen
import com.tinyai.geekbox.feature.device.VolumeScreen
import com.tinyai.geekbox.feature.devtools.Base64Screen
import com.tinyai.geekbox.feature.devtools.BarcodeScreen
import com.tinyai.geekbox.feature.devtools.ColorScreen
import com.tinyai.geekbox.feature.devtools.CronScreen
import com.tinyai.geekbox.feature.devtools.DevToolsHomeScreen
import com.tinyai.geekbox.feature.devtools.DiffScreen
import com.tinyai.geekbox.feature.devtools.HashScreen
import com.tinyai.geekbox.feature.devtools.HexScreen
import com.tinyai.geekbox.feature.devtools.JsonToolScreen
import com.tinyai.geekbox.feature.devtools.JwtScreen
import com.tinyai.geekbox.feature.devtools.QrScreen
import com.tinyai.geekbox.feature.devtools.RadixScreen
import com.tinyai.geekbox.feature.devtools.RandomScreen
import com.tinyai.geekbox.feature.devtools.RegexScreen
import com.tinyai.geekbox.feature.devtools.SubnetScreen
import com.tinyai.geekbox.feature.devtools.TextToolScreen
import com.tinyai.geekbox.feature.devtools.TimestampScreen
import com.tinyai.geekbox.feature.devtools.UrlScreen
import com.tinyai.geekbox.feature.devtools.UuidScreen
import com.tinyai.geekbox.feature.devtools.XmlScreen
import com.tinyai.geekbox.feature.media.AudioScreen
import com.tinyai.geekbox.feature.media.MediaHomeScreen
import com.tinyai.geekbox.feature.media.VideoInfoScreen
import com.tinyai.geekbox.feature.network.AppTrafficScreen
import com.tinyai.geekbox.feature.network.CertScreen
import com.tinyai.geekbox.feature.network.ConnectionsScreen
import com.tinyai.geekbox.feature.network.DnsBenchScreen
import com.tinyai.geekbox.feature.network.DnsScreen
import com.tinyai.geekbox.feature.network.DownloaderScreen
import com.tinyai.geekbox.feature.network.HttpScreen
import com.tinyai.geekbox.feature.network.LanScanScreen
import com.tinyai.geekbox.feature.network.ListenPortsScreen
import com.tinyai.geekbox.feature.network.NetworkHomeScreen
import com.tinyai.geekbox.feature.network.NetworkInfoScreen
import com.tinyai.geekbox.feature.network.NetworkSpeedScreen
import com.tinyai.geekbox.feature.network.NtpScreen
import com.tinyai.geekbox.feature.network.PingScreen
import com.tinyai.geekbox.feature.network.PortScanScreen
import com.tinyai.geekbox.feature.network.PtrScreen
import com.tinyai.geekbox.feature.network.RedirectScreen
import com.tinyai.geekbox.feature.network.TracerouteScreen
import com.tinyai.geekbox.feature.network.WhoisScreen
import com.tinyai.geekbox.feature.network.WifiScanScreen
import com.tinyai.geekbox.feature.network.WolScreen
import com.tinyai.geekbox.feature.system.AboutScreen
import com.tinyai.geekbox.feature.system.AppManagerScreen
import com.tinyai.geekbox.feature.system.AppStorageScreen
import com.tinyai.geekbox.feature.system.DumpsysScreen
import com.tinyai.geekbox.feature.system.IntentScreen
import com.tinyai.geekbox.feature.system.LogcatScreen
import com.tinyai.geekbox.feature.system.MountsScreen
import com.tinyai.geekbox.feature.system.ProcessManagerScreen
import com.tinyai.geekbox.feature.system.PropsScreen
import com.tinyai.geekbox.feature.system.RootScreen
import com.tinyai.geekbox.feature.system.ShellScreen
import com.tinyai.geekbox.feature.system.ShizukuScreen
import com.tinyai.geekbox.feature.system.ShortcutsScreen
import com.tinyai.geekbox.feature.system.SystemHomeScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("device", "设备", Icons.Filled.PhoneAndroid),
    Tab("ai", "AI", Icons.Filled.SmartToy),
    Tab("apps", "应用", Icons.Filled.Apps),
    Tab("media", "媒体", Icons.Filled.MusicNote),
    Tab("devtools", "开发", Icons.Filled.Code),
    Tab("network", "网络", Icons.Filled.Lan),
    Tab("system", "系统", Icons.Filled.Settings)
)

@Composable
fun GeekBoxApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route
    val bottomRoutes = tabs.map { it.route }.toSet()
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (current in bottomRoutes) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                if (current != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "device",
            modifier = Modifier.padding(padding)
        ) {
            composable("device") { DeviceHomeScreen { navController.navigate(it.route) } }
            composable("ai") {
                AiHomeScreen { name, url ->
                    navController.navigate("ai/web?name=${android.net.Uri.encode(name)}&url=${android.net.Uri.encode(url)}")
                }
            }
            composable(
                "ai/web?name={name}&url={url}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType; defaultValue = "AI" },
                    navArgument("url") { type = NavType.StringType; defaultValue = "https://chat.deepseek.com/" }
                )
            ) { entry ->
                AiWebScreen(
                    title = entry.arguments?.getString("name") ?: "AI",
                    url = entry.arguments?.getString("url") ?: "https://chat.deepseek.com/",
                    onBack = back
                )
            }
            composable("device/cpu") { CpuDetailScreen(back) }
            composable("device/gpu") { GpuScreen(back) }
            composable("device/memory") { MemoryDetailScreen(back) }
            composable("device/battery") { BatteryDetailScreen(back) }
            composable("device/storage") { StorageDetailScreen(back) }
            composable("device/screen") { ScreenInfoScreen(back) }
            composable("device/camera") { CameraInfoScreen(back) }
            composable("device/flashlight") { FlashlightScreen(back) }
            composable("device/volume") { VolumeScreen(back) }
            composable("device/screentest") { ScreenTestScreen(back) }
            composable("device/report") { DeviceReportScreen(back) }
            composable("device/thermal") { ThermalScreen(back) }
            composable("device/sensors") {
                SensorsScreen(onBack = back, onOpenLive = { navController.navigate("device/sensors/live/$it") })
            }
            composable(
                "device/sensors/live/{type}",
                arguments = listOf(navArgument("type") { type = NavType.IntType })
            ) { entry ->
                SensorLiveScreen(onBack = back, sensorType = entry.arguments?.getInt("type") ?: 0)
            }

            composable("apps") { AppsHomeScreen { navController.navigate(it) } }
            composable(
                "apps/detail/{pkg}",
                arguments = listOf(navArgument("pkg") { type = NavType.StringType })
            ) { entry ->
                AppDetailScreen(packageName = entry.arguments?.getString("pkg").orEmpty(), onBack = back)
            }
            composable("apps/apk") { ApkInspectScreen(back) }

            composable("media") { MediaHomeScreen { navController.navigate(it) } }
            composable("media/audio") { AudioScreen(back) }
            composable("media/video") { VideoInfoScreen(back) }

            composable("devtools") { DevToolsHomeScreen { navController.navigate(it) } }
            composable("devtools/json") { JsonToolScreen(back) }
            composable("devtools/xml") { XmlScreen(back) }
            composable("devtools/base64") { Base64Screen(back) }
            composable("devtools/hash") { HashScreen(back) }
            composable("devtools/hex") { HexScreen(back) }
            composable("devtools/regex") { RegexScreen(back) }
            composable("devtools/url") { UrlScreen(back) }
            composable("devtools/timestamp") { TimestampScreen(back) }
            composable("devtools/uuid") { UuidScreen(back) }
            composable("devtools/qr") { QrScreen(back) }
            composable("devtools/color") { ColorScreen(back) }
            composable("devtools/text") { TextToolScreen(back) }
            composable("devtools/diff") { DiffScreen(back) }
            composable("devtools/radix") { RadixScreen(back) }
            composable("devtools/cron") { CronScreen(back) }
            composable("devtools/random") { RandomScreen(back) }
            composable("devtools/barcode") { BarcodeScreen(back) }
            composable("devtools/subnet") { SubnetScreen(back) }
            composable("devtools/jwt") { JwtScreen(back) }

            composable("network") { NetworkHomeScreen { navController.navigate(it) } }
            composable("network/info") { NetworkInfoScreen(back) }
            composable("network/speed") { NetworkSpeedScreen(back) }
            composable("network/connections") { ConnectionsScreen(back) }
            composable("network/traffic") { AppTrafficScreen(back) }
            composable("network/wifi") { WifiScanScreen(back) }
            composable("network/dns") { DnsScreen(back) }
            composable("network/dnsbench") { DnsBenchScreen(back) }
            composable("network/cert") { CertScreen(back) }
            composable("network/whois") { WhoisScreen(back) }
            composable("network/download") { DownloaderScreen(back) }
            composable("network/ntp") { NtpScreen(back) }
            composable("network/wol") { WolScreen(back) }
            composable("network/listen") { ListenPortsScreen(back) }
            composable("network/ptr") { PtrScreen(back) }
            composable("network/redirect") { RedirectScreen(back) }
            composable("network/ping") { PingScreen(back) }
            composable("network/traceroute") { TracerouteScreen(back) }
            composable("network/http") { HttpScreen(back) }
            composable("network/portscan") { PortScanScreen(back) }
            composable("network/lanscan") { LanScanScreen(back) }

            composable("system") { SystemHomeScreen { navController.navigate(it) } }
            composable("system/shizuku") { ShizukuScreen(back) }
            composable("system/shell") { ShellScreen(back) }
            composable("system/logcat") { LogcatScreen(back) }
            composable("system/intent") { IntentScreen(back) }
            composable("system/root") { RootScreen(back) }
            composable("system/props") { PropsScreen(back) }
            composable("system/processes") { ProcessManagerScreen(back) }
            composable("system/appmanager") { AppManagerScreen(back) }
            composable("system/appstorage") { AppStorageScreen(back) }
            composable("system/dumpsys") { DumpsysScreen(back) }
            composable("system/mounts") { MountsScreen(back) }
            composable("system/shortcuts") { ShortcutsScreen(back) }
            composable("system/about") { AboutScreen(back) }
        }
    }
}
