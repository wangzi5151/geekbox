package com.tinyai.geekbox.feature.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.tinyai.geekbox.core.util.SysFs

data class DeviceOverview(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val board: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val kernel: String,
    val buildId: String,
    val buildType: String,
    val fingerprint: String,
    val abis: List<String>,
    val uptimeMillis: Long
)

data class CoreInfo(
    val index: Int,
    val curKhz: Long?,
    val minKhz: Long?,
    val maxKhz: Long?,
    val governor: String?
)

data class CpuInfo(
    val cores: List<CoreInfo>,
    val loadAvg: String,
    val hardware: String
)

data class MemDetail(val label: String, val bytes: Long)

data class MemoryInfo(
    val total: Long,
    val available: Long,
    val used: Long,
    val lowMemory: Boolean,
    val threshold: Long,
    val swapTotal: Long,
    val swapFree: Long,
    val details: List<MemDetail>
)

data class BatteryInfo(
    val level: Int,
    val status: String,
    val health: String,
    val plugged: String,
    val temperatureC: Float,
    val voltageMv: Int,
    val technology: String,
    val present: Boolean,
    val capacityMah: Int
)

data class StorageInfo(val label: String, val path: String, val total: Long, val free: Long)

data class SensorInfo(
    val name: String,
    val vendor: String,
    val type: Int,
    val typeName: String,
    val maxRange: Float,
    val resolution: Float,
    val power: Float,
    val wakeUp: Boolean
)

data class ThermalZone(val name: String, val tempC: Float)

data class GpuInfo(
    val model: String?,
    val curKhz: Long?,
    val maxKhz: Long?,
    val minKhz: Long?,
    val extras: List<Pair<String, String>>,
    val note: String?
)

data class ScreenInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val xdpi: Float,
    val ydpi: Float,
    val refreshRate: Float,
    val refreshRates: List<Float>,
    val hdr: List<String>,
    val sizeClass: String
)

data class CameraInfo(
    val id: String,
    val facing: String,
    val hardwareLevel: String,
    val orientation: Int,
    val flashAvailable: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val focalLengths: List<Float>,
    val afModes: Int
)

class DeviceRepository(private val context: Context) {

    fun overview(): DeviceOverview = DeviceOverview(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        hardware = Build.HARDWARE,
        board = Build.BOARD,
        androidVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH,
        kernel = System.getProperty("os.version") ?: "-",
        buildId = Build.DISPLAY,
        buildType = Build.TYPE,
        fingerprint = Build.FINGERPRINT,
        abis = Build.SUPPORTED_ABIS.toList(),
        uptimeMillis = SystemClock.elapsedRealtime()
    )

    fun cpu(): CpuInfo {
        val count = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val cores = (0 until count).map { i ->
            val base = "/sys/devices/system/cpu/cpu$i/cpufreq"
            CoreInfo(
                index = i,
                curKhz = SysFs.readLong("$base/scaling_cur_freq"),
                minKhz = SysFs.readLong("$base/cpuinfo_min_freq"),
                maxKhz = SysFs.readLong("$base/cpuinfo_max_freq"),
                governor = SysFs.read("$base/scaling_governor")
            )
        }
        return CpuInfo(
            cores = cores,
            loadAvg = SysFs.read("/proc/loadavg") ?: "-",
            hardware = SysFs.read("/proc/cpuinfo")
                ?.lineSequence()
                ?.firstOrNull { it.startsWith("Hardware") }
                ?.substringAfter(":")?.trim() ?: Build.HARDWARE
        )
    }

    fun memory(): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val meminfo = parseMeminfo()
        return MemoryInfo(
            total = mi.totalMem,
            available = mi.availMem,
            used = (mi.totalMem - mi.availMem).coerceAtLeast(0),
            lowMemory = mi.lowMemory,
            threshold = mi.threshold,
            swapTotal = meminfo["SwapTotal"] ?: 0,
            swapFree = meminfo["SwapFree"] ?: 0,
            details = listOf(
                MemDetail("MemFree", meminfo["MemFree"] ?: 0),
                MemDetail("Buffers", meminfo["Buffers"] ?: 0),
                MemDetail("Cached", meminfo["Cached"] ?: 0),
                MemDetail("Active", meminfo["Active"] ?: 0),
                MemDetail("Inactive", meminfo["Inactive"] ?: 0),
                MemDetail("Dirty", meminfo["Dirty"] ?: 0),
                MemDetail("Slab", meminfo["Slab"] ?: 0)
            ).filter { it.bytes > 0 }
        )
    }

    private fun parseMeminfo(): Map<String, Long> {
        val text = SysFs.read("/proc/meminfo") ?: return emptyMap()
        val result = LinkedHashMap<String, Long>()
        text.lineSequence().forEach { line ->
            val parts = line.split(':')
            if (parts.size == 2) {
                val kb = parts[1].trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()
                if (kb != null) result[parts[0].trim()] = kb * 1024
            }
        }
        return result
    }

    fun battery(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "未知"
        }
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }
        val plugged = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "交流电"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线"
            else -> "未连接"
        }
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "-"
        val present = intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true
        val capacity = readBatteryCapacity()
        return BatteryInfo(percent, status, health, plugged, temp, voltage, tech, present, capacity)
    }

    private fun readBatteryCapacity(): Int {
        val candidates = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/bms/charge_full_design"
        )
        candidates.forEach { path ->
            SysFs.readLong(path)?.let { micro ->
                if (micro > 0) return (micro / 1000).toInt()
            }
        }
        return -1
    }

    fun storage(): List<StorageInfo> {
        val list = ArrayList<StorageInfo>()
        addStorage(list, "内部存储", Environment.getDataDirectory().absolutePath)
        try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                addStorage(list, "外部存储", Environment.getExternalStorageDirectory().absolutePath)
            }
        } catch (_: Throwable) {
        }
        return list
    }

    private fun addStorage(list: MutableList<StorageInfo>, label: String, path: String) {
        try {
            val stat = StatFs(path)
            list.add(StorageInfo(label, path, stat.blockCountLong * stat.blockSizeLong, stat.availableBlocksLong * stat.blockSizeLong))
        } catch (_: Throwable) {
        }
    }

    fun sensors(): List<SensorInfo> {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getSensorList(Sensor.TYPE_ALL)
            .map { s ->
                SensorInfo(
                    name = s.name,
                    vendor = s.vendor,
                    type = s.type,
                    typeName = typeName(s.type),
                    maxRange = s.maximumRange,
                    resolution = s.resolution,
                    power = s.power,
                    wakeUp = s.isWakeUpSensor
                )
            }
            .sortedBy { it.typeName }
    }

    fun thermal(): List<ThermalZone> {
        val base = "/sys/class/thermal"
        return SysFs.listDirs(base)
            .filter { it.startsWith("thermal_zone") }
            .mapNotNull { zone ->
                val name = SysFs.read("$base/$zone/type")
                val temp = SysFs.readLong("$base/$zone/temp")
                if (temp == null) null
                else ThermalZone(name ?: zone, normalizeTemp(temp))
            }
            .sortedBy { it.tempC }
    }

    fun gpu(): GpuInfo {
        val model = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/class/kgsl/kgsl-3d0/device/gpu_model",
            "/sys/class/misc/mali0/device/gpuinfo",
            "/sys/kernel/gpu/gpu_model"
        ).firstNotNullOfOrNull { SysFs.read(it) }

        val extras = ArrayList<Pair<String, String>>()
        var cur = SysFs.readLong("/sys/class/kgsl/kgsl-3d0/gpuclk")
        var max = SysFs.readLong("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq")
        var min = SysFs.readLong("/sys/class/kgsl/kgsl-3d0/devfreq/min_freq")

        val devfreq = SysFs.listDirs("/sys/class/devfreq")
        devfreq.filter { n ->
            n.contains("gpu", true) || n.contains("kgsl", true) ||
                n.contains("mali", true) || n.contains("gpufreq", true)
        }.forEach { dir ->
            val base = "/sys/class/devfreq/$dir"
            if (cur == null) cur = SysFs.readLong("$base/cur_freq")
            if (max == null) max = SysFs.readLong("$base/max_freq")
            if (min == null) min = SysFs.readLong("$base/min_freq")
            SysFs.read("$base/governor")?.let { extras.add(dir to "governor=$it") }
            SysFs.read("$base/available_frequencies")?.let { extras.add("$dir 可用频率" to it) }
        }

        SysFs.read("/proc/gpuinfo")?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(8)
            ?.forEach { extras.add("gpuinfo" to it) }

        val note = if (model == null && cur == null && extras.isEmpty()) {
            "未读取到 GPU 信息，多数设备需 Root。"
        } else null

        return GpuInfo(
            model = model,
            curKhz = cur?.let { it / 1000 },
            maxKhz = max?.let { it / 1000 },
            minKhz = min?.let { it / 1000 },
            extras = extras.distinct(),
            note = note
        )
    }

    fun screen(): ScreenInfo {
        val dm = context.resources.displayMetrics
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        }
        val refreshRates = try {
            display?.supportedModes?.map { it.refreshRate }?.distinct()?.sorted() ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
        val hdr = try {
            display?.hdrCapabilities?.supportedHdrTypes?.map { hdrTypeName(it) } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
        val sizeClass = when {
            dm.densityDpi >= 480 -> "xxhdpi+"
            dm.densityDpi >= 320 -> "xhdpi"
            dm.densityDpi >= 240 -> "hdpi"
            else -> "mdpi"
        }
        return ScreenInfo(
            widthPx = dm.widthPixels,
            heightPx = dm.heightPixels,
            densityDpi = dm.densityDpi,
            density = dm.density,
            xdpi = dm.xdpi,
            ydpi = dm.ydpi,
            refreshRate = display?.refreshRate ?: 0f,
            refreshRates = refreshRates,
            hdr = hdr,
            sizeClass = sizeClass
        )
    }

    fun cameras(): List<CameraInfo> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            ?: return emptyList()
        return try {
            manager.cameraIdList.mapNotNull { id ->
                val c = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: return@mapNotNull null
                val map = runCatching { c.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) }.getOrNull()
                val jpegSizes = runCatching { map?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.toList() }.getOrNull().orEmpty()
                val largest = jpegSizes.maxByOrNull { it.width.toLong() * it.height }
                CameraInfo(
                    id = id,
                    facing = when (c.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)) {
                        android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT -> "前置"
                        android.hardware.camera2.CameraMetadata.LENS_FACING_BACK -> "后置"
                        android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL -> "外接"
                        else -> "未知"
                    },
                    hardwareLevel = when (c.get(android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                        android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                        else -> "未知"
                    },
                    orientation = c.get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION) ?: 0,
                    flashAvailable = c.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false,
                    maxWidth = largest?.width ?: 0,
                    maxHeight = largest?.height ?: 0,
                    focalLengths = runCatching {
                        c.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList()
                    }.getOrNull().orEmpty(),
                    afModes = runCatching {
                        c.get(android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.size ?: 0
                    }.getOrDefault(0)
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun hdrTypeName(type: Int): String = when (type) {
        1 -> "Dolby Vision"
        2 -> "HDR10"
        3 -> "HLG"
        4 -> "HDR10+"
        else -> "HDR($type)"
    }

    private fun normalizeTemp(raw: Long): Float =
        when {
            raw >= 100_000 -> raw / 1000f
            raw >= 1_000 -> raw / 10f
            else -> raw.toFloat()
        }

    private fun typeName(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> "加速度计"
        Sensor.TYPE_GYROSCOPE -> "陀螺仪"
        Sensor.TYPE_MAGNETIC_FIELD -> "磁力计"
        Sensor.TYPE_LIGHT -> "光线"
        Sensor.TYPE_PRESSURE -> "气压计"
        Sensor.TYPE_PROXIMITY -> "接近"
        Sensor.TYPE_GRAVITY -> "重力"
        Sensor.TYPE_LINEAR_ACCELERATION -> "线性加速度"
        Sensor.TYPE_ROTATION_VECTOR -> "旋转矢量"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "湿度"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "环境温度"
        Sensor.TYPE_STEP_COUNTER -> "计步器"
        Sensor.TYPE_STEP_DETECTOR -> "步伐检测"
        Sensor.TYPE_HEART_RATE -> "心率"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "游戏旋转矢量"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "显著运动"
        else -> "其他($type)"
    }
}
