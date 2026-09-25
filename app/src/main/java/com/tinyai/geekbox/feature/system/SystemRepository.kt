package com.tinyai.geekbox.feature.system

import android.content.Context
import android.content.pm.PackageManager
import com.tinyai.geekbox.core.shizuku.PrivilegedExec
import com.tinyai.geekbox.core.shizuku.ShizukuBridge
import com.tinyai.geekbox.core.util.Shell
import com.tinyai.geekbox.core.util.SysFs
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class RootStatus(
    val available: Boolean,
    val suPath: String?,
    val uidOutput: String?,
    val selinux: String?,
    val shizukuInstalled: Boolean,
    val details: String
)

data class ProcessEntry(val pid: Int, val name: String)

data class MountInfo(val device: String, val mountPoint: String, val fsType: String, val options: String)

class SystemRepository(private val context: Context) {

    fun rootStatus(): RootStatus {
        val suPath = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/system/sbin/su", "/vendor/bin/su", "/debug_ramdisk/su"
        ).firstOrNull { File(it).exists() || File(it).canExecute() }
        val selinux = SysFs.read("/sys/fs/selinux/enforce")?.let { if (it == "1") "Enforcing" else "Permissive" }
            ?: Shell.run("getenforce", 5).stdout.ifBlank { null }
        val shizuku = isPackageInstalled("moe.shizuku.privileged.api")
        val details = when {
            suPath != null -> "检测到 su（$suPath）。首次使用时会请求 Root 授权。"
            else -> "未检测到 Root，部分高级功能将以只读方式降级。"
        }
        return RootStatus(suPath != null, suPath, null, selinux, shizuku, details)
    }

    fun requestRoot(): Boolean {
        val result = Shell.runRoot("id", 10)
        return result.stdout.contains("uid=0") || result.combined.contains("uid=0")
    }

    fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Throwable) {
        false
    }

    fun systemProperties(): List<Pair<String, String>> {
        val result = Shell.run("getprop", 10)
        return result.stdout.lineSequence()
            .mapNotNull { line ->
                val match = Regex("\\[(.+?)\\]: \\[(.*?)\\]").find(line)
                if (match != null) match.groupValues[1] to match.groupValues[2] else null
            }
            .sortedBy { it.first }
            .toList()
    }

    fun mounts(): List<MountInfo> {
        val text = PrivilegedExec.run("cat /proc/mounts", 10).stdout.ifBlank {
            SysFs.read("/proc/mounts") ?: ""
        }
        return text.lineSequence().mapNotNull { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 4) return@mapNotNull null
            MountInfo(parts[0], parts[1], parts[2], parts[3])
        }.toList()
    }

    fun processesViaShizuku(): List<ProcessEntry> {
        val output = ShizukuBridge.runShell("ps -A").stdout
        return parsePs(output)
    }

    private fun parsePs(output: String): List<ProcessEntry> =
        output.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 2) return@mapNotNull null
                val pid = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                ProcessEntry(pid, parts.last())
            }
            .sortedBy { it.pid }
            .toList()

    fun processes(): List<ProcessEntry> {
        val procDirs = File("/proc").listFiles()?.filter { it.isDirectory && it.name.all { c -> c.isDigit() } }
            ?: return emptyList()
        return procDirs.mapNotNull { dir ->
            val pid = dir.name.toIntOrNull() ?: return@mapNotNull null
            val cmdline = try {
                File(dir, "cmdline").readText().trim().trim('\u0000').ifBlank { null }
            } catch (_: Throwable) {
                null
            } ?: return@mapNotNull null
            ProcessEntry(pid, cmdline)
        }.sortedBy { it.pid }
    }

    suspend fun logcat(
        root: Boolean,
        clear: Boolean,
        lineLimit: Int,
        follow: Boolean,
        onLine: (String) -> Unit
    ) {
        if (clear) {
            if (root) Shell.runRoot("logcat -c", 5) else Shell.run("logcat -c", 5)
        }
        val cmd = buildString {
            append("logcat -v time")
            if (!follow && lineLimit > 0) append(" -t $lineLimit")
            if (follow) append(" -T 1")
        }
        val argv = if (root) arrayOf("su", "-c", cmd) else arrayOf("sh", "-c", cmd)
        val process = try {
            ProcessBuilder(*argv).redirectErrorStream(true).start()
        } catch (t: Throwable) {
            onLine("无法启动 logcat: ${t.message}")
            return
        }
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            lines.forEach { onLine(it) }
        }
        process.waitFor()
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            false
        }
    }
}
