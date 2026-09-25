package com.tinyai.geekbox.feature.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val versionName: String,
    val versionCode: Long,
    val uid: Int,
    val apkPath: String,
    val sizeBytes: Long,
    val enabled: Boolean
)

data class PermissionItem(val name: String, val granted: Boolean)

data class ComponentItem(
    val name: String,
    val exported: Boolean,
    val permission: String?,
    val enabled: Boolean
)

data class AppDetails(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val uid: Int,
    val isSystem: Boolean,
    val isDebug: Boolean,
    val enabled: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkPath: String,
    val dataDir: String,
    val nativeLibDir: String,
    val installLocation: String,
    val apkSize: Long,
    val permissions: List<PermissionItem>,
    val activities: List<ComponentItem>,
    val services: List<ComponentItem>,
    val receivers: List<ComponentItem>,
    val providers: List<ComponentItem>,
    val signatures: List<SignatureInfo>,
    val abis: List<String>
)

data class SignatureInfo(val algorithm: String, val sha1: String, val sha256: String, val valid: Boolean)

class AppRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun listApps(includeSystem: Boolean): List<AppEntry> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        return packages.mapNotNull { pi ->
            val app = pi.applicationInfo ?: return@mapNotNull null
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null
            AppEntry(
                packageName = pi.packageName,
                label = runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(pi.packageName),
                isSystem = isSystem,
                versionName = pi.versionName ?: "-",
                versionCode = versionCodeOf(pi),
                uid = app.uid,
                apkPath = app.sourceDir ?: "",
                sizeBytes = app.sourceDir?.let { File(it).length() } ?: 0L,
                enabled = app.enabled
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun details(packageName: String): AppDetails {
        val flags = detailsFlags()
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags.toInt())
        }
        return buildDetails(pi)
    }

    fun archiveDetails(apkPath: String): AppDetails? {
        val flags = detailsFlags()
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkPath, flags.toInt())
        } ?: return null
        pi.applicationInfo?.let {
            it.sourceDir = apkPath
            it.publicSourceDir = apkPath
        }
        return buildDetails(pi)
    }

    private fun detailsFlags(): Long {
        var flags = PackageManager.GET_ACTIVITIES.toLong() or
            PackageManager.GET_SERVICES.toLong() or
            PackageManager.GET_RECEIVERS.toLong() or
            PackageManager.GET_PROVIDERS.toLong() or
            PackageManager.GET_PERMISSIONS.toLong() or
            PackageManager.GET_META_DATA.toLong()
        flags = flags or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES.toLong()
        }
        return flags
    }

    @Suppress("DEPRECATION")
    private fun buildDetails(pi: PackageInfo): AppDetails {
        val app = pi.applicationInfo
        val label = app?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrDefault(pi.packageName) } ?: pi.packageName
        val requested = pi.requestedPermissions ?: emptyArray()
        val reqFlags = pi.requestedPermissionsFlags ?: IntArray(0)
        val permissions = requested.mapIndexed { i, perm ->
            val flag = reqFlags.getOrElse(i) { 0 }
            PermissionItem(perm, (flag and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
        }.sortedBy { it.name }

        val activities = pi.activities?.map { toComponent(it.name, it.exported, it.permission, it.enabled) } ?: emptyList()
        val services = pi.services?.map { toComponent(it.name, it.exported, it.permission, it.enabled) } ?: emptyList()
        val receivers = pi.receivers?.map { toComponent(it.name, it.exported, it.permission, it.enabled) } ?: emptyList()
        val providers = pi.providers?.map {
            toComponent(it.name, it.exported, it.readPermission ?: it.writePermission, it.enabled)
        } ?: emptyList()

        val signatures = readSignatures(pi)
        val installLocation = "-"
        val abis = Build.SUPPORTED_ABIS.toList()

        return AppDetails(
            packageName = pi.packageName,
            label = label,
            versionName = pi.versionName ?: "-",
            versionCode = versionCodeOf(pi),
            targetSdk = app?.targetSdkVersion ?: 0,
            minSdk = app?.minSdkVersion ?: 0,
            uid = app?.uid ?: 0,
            isSystem = app != null && (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            isDebug = app != null && (app.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            enabled = app?.enabled ?: true,
            firstInstallTime = pi.firstInstallTime,
            lastUpdateTime = pi.lastUpdateTime,
            apkPath = app?.sourceDir ?: "-",
            dataDir = app?.dataDir ?: "-",
            nativeLibDir = app?.nativeLibraryDir ?: "-",
            installLocation = installLocation,
            apkSize = app?.sourceDir?.let { File(it).length() } ?: 0L,
            permissions = permissions,
            activities = activities,
            services = services,
            receivers = receivers,
            providers = providers,
            signatures = signatures,
            abis = abis
        )
    }

    private fun toComponent(name: String, exported: Boolean, permission: String?, enabled: Boolean) =
        ComponentItem(name, exported, permission, enabled)

    @Suppress("DEPRECATION")
    private fun readSignatures(pi: PackageInfo): List<SignatureInfo> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pi.signingInfo ?: return emptyList()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            pi.signatures
        }
        return signatures?.map { sig ->
            val bytes = sig.toByteArray()
            SignatureInfo(
                algorithm = sig.toCharsString().let { "证书" },
                sha1 = digest("SHA-1", bytes),
                sha256 = digest("SHA-256", bytes),
                valid = true
            )
        } ?: emptyList()
    }

    private fun digest(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).joinToString(":") { "%02X".format(it) }

    private fun versionCodeOf(pi: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()

    fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: Throwable) {
        false
    }
}
