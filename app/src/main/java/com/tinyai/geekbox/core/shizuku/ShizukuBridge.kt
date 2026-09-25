package com.tinyai.geekbox.core.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinyai.geekbox.core.util.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread

class ShizukuProcess internal constructor(
    private val remote: IRemoteProcess,
    val stdout: InputStream,
    val stderr: InputStream
) {
    val exitCode: Int get() = runCatching { remote.exitValue() }.getOrDefault(-1)
    fun waitFor(): Int = runCatching { remote.waitFor() }.getOrDefault(-1)
    fun destroy() {
        runCatching { remote.destroy() }
    }
}

object ShizukuBridge {

    const val PACKAGE = "moe.shizuku.privileged.api"

    var revision by mutableStateOf(0)
        private set

    var lastPermissionResult by mutableStateOf<Boolean?>(null)
        private set

    private var initialized = false
    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null
    private var binderReceived: Shizuku.OnBinderReceivedListener? = null
    private var binderDead: Shizuku.OnBinderDeadListener? = null

    fun init() {
        if (initialized) return
        initialized = true
        val permission = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            lastPermissionResult = grantResult == PackageManager.PERMISSION_GRANTED
            revision++
        }
        val received = Shizuku.OnBinderReceivedListener { revision++ }
        val dead = Shizuku.OnBinderDeadListener { revision++ }
        permissionListener = permission
        binderReceived = received
        binderDead = dead
        runCatching { Shizuku.addRequestPermissionResultListener(permission) }
        runCatching { Shizuku.addBinderReceivedListener(received) }
        runCatching { Shizuku.addBinderDeadListener(dead) }
    }

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (_: Throwable) {
        false
    }

    fun binderAlive(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isPreV11(): Boolean = runCatching { Shizuku.isPreV11() }.getOrDefault(false)

    fun checkSelfPermission(): Int =
        runCatching { Shizuku.checkSelfPermission() }.getOrDefault(PackageManager.PERMISSION_DENIED)

    fun isAuthorized(): Boolean =
        binderAlive() && !isPreV11() && checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun uid(): Int? = runCatching { Shizuku.getUid() }.getOrNull()

    fun version(): Int? = runCatching { Shizuku.getVersion() }.getOrNull()

    fun uidLabel(): String = when (val u = uid()) {
        null -> "-"
        0 -> "root"
        2000 -> "shell (ADB)"
        else -> "uid $u"
    }

    fun requestPermission(requestCode: Int): Boolean = runCatching {
        if (!binderAlive() || isPreV11()) return false
        Shizuku.requestPermission(requestCode)
        true
    }.getOrDefault(false)

    private fun service(): IShizukuService? = runCatching {
        if (!isAuthorized()) null else IShizukuService.Stub.asInterface(Shizuku.getBinder())
    }.getOrNull()

    fun openShell(command: String): ShizukuProcess? {
        val service = service() ?: return null
        val remote = runCatching {
            service.newProcess(arrayOf("sh", "-c", command), null, null)
        }.getOrNull() ?: return null
        return ShizukuProcess(
            remote = remote,
            stdout = ParcelFileDescriptor.AutoCloseInputStream(runCatching { remote.inputStream }.getOrThrow()),
            stderr = ParcelFileDescriptor.AutoCloseInputStream(runCatching { remote.errorStream }.getOrThrow())
        )
    }

    fun runShell(command: String, timeoutSeconds: Long = 30): ShellResult {
        val process = openShell(command) ?: return ShellResult(-1, "", "Shizuku 未授权或 Binder 未运行")
        var stdout = ""
        var stderr = ""
        val outThread = thread { stdout = readAll(process.stdout) }
        val errThread = thread { stderr = readAll(process.stderr) }
        val waiter = thread { process.waitFor() }
        var timedOut = false
        waiter.join(timeoutSeconds.coerceAtLeast(1) * 1000)
        if (waiter.isAlive) {
            timedOut = true
            process.destroy()
        }
        outThread.join(1500)
        errThread.join(1500)
        process.destroy()
        return ShellResult(process.exitCode, stdout, stderr, timedOut)
    }

    suspend fun streamShell(command: String, onLine: (String) -> Unit) {
        val process = openShell(command) ?: run {
            onLine("[Shizuku 未授权或 Binder 未运行]")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(process.stdout))
                while (true) {
                    if (!currentCoroutineContext().isActive) break
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            } finally {
                runCatching { process.destroy() }
            }
        }
    }

    private fun readAll(stream: InputStream): String = try {
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
    } catch (_: Throwable) {
        ""
    }
}
