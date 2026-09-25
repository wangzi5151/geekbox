package com.tinyai.geekbox.feature.system

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager

data class AppTraffic(val uid: Int, val rx: Long, val tx: Long) {
    val total: Long get() = rx + tx
}

data class AppStorage(val uid: Int, val dataBytes: Long, val cacheBytes: Long) {
    val total: Long get() = dataBytes + cacheBytes
}

class AppUsageRepository(private val context: Context) {

    fun hasUsageAccess(): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Throwable) {
        false
    }

    fun trafficByUid(sinceMillis: Long): List<AppTraffic> {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return emptyList()
        val end = System.currentTimeMillis()
        val result = HashMap<Int, LongArray>()
        val types = listOf(
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_MOBILE,
            ConnectivityManager.TYPE_ETHERNET
        )
        types.forEach { type ->
            val stats = runCatching { nsm.querySummary(type, null, sinceMillis, end) }.getOrNull() ?: return@forEach
            try {
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val entry = result.getOrPut(bucket.uid) { longArrayOf(0, 0) }
                    entry[0] += bucket.rxBytes
                    entry[1] += bucket.txBytes
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { stats.close() }
            }
        }
        return result.map { (uid, v) -> AppTraffic(uid, v[0], v[1]) }
            .filter { it.uid != Process.myUid() && it.total > 0 }
            .sortedByDescending { it.total }
    }

    fun labelForUid(uid: Int): String {
        val pm = context.packageManager
        val packages = pm.getPackagesForUid(uid) ?: return "uid $uid"
        val pkg = packages.firstOrNull() ?: return "uid $uid"
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    }

    fun packageForUid(uid: Int): String? =
        context.packageManager.getPackagesForUid(uid)?.firstOrNull()

    fun storageByUid(): List<AppStorage> {
        val ssm = context.getSystemService(StorageStatsManager::class.java) ?: return emptyList()
        val pm = context.packageManager
        val seen = HashSet<Int>()
        val result = ArrayList<AppStorage>()
        pm.getInstalledApplications(0).forEach { app ->
            if (!seen.add(app.uid)) return@forEach
            val stat = runCatching { ssm.queryStatsForUid(StorageManager.UUID_DEFAULT, app.uid) }.getOrNull() ?: return@forEach
            if (stat.dataBytes > 0 || stat.cacheBytes > 0) {
                result.add(AppStorage(app.uid, stat.dataBytes, stat.cacheBytes))
            }
        }
        return result.sortedByDescending { it.total }
    }
}
