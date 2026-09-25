package com.tinyai.geekbox.core.util

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false
) {
    val ok: Boolean get() = exitCode == 0 && !timedOut
    val combined: String get() = buildString {
        if (stdout.isNotBlank()) append(stdout.trim())
        if (stderr.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(stderr.trim())
        }
    }
}

object Shell {

    fun run(command: String, timeoutSeconds: Long = 20): ShellResult =
        execute(arrayOf("sh", "-c", command), timeoutSeconds)

    fun runRoot(command: String, timeoutSeconds: Long = 20): ShellResult =
        execute(arrayOf("su", "-c", command), timeoutSeconds)

    fun isRootAvailable(): Boolean = try {
        val result = execute(arrayOf("su", "-c", "id"), 5)
        result.stdout.contains("uid=0")
    } catch (_: Throwable) {
        false
    }

    fun suPath(): String? = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
        "/system/sbin/su", "/vendor/bin/su", "/debug_ramdisk/su"
    ).firstOrNull { File(it).exists() }

    fun hasSu(): Boolean = suPath() != null

    private fun execute(argv: Array<String>, timeoutSeconds: Long): ShellResult {
        val process = try {
            ProcessBuilder(*argv).redirectErrorStream(false).start()
        } catch (t: Throwable) {
            return ShellResult(-1, "", t.message ?: "failed to start process")
        }
        var timedOut = false
        try {
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                timedOut = true
                process.destroyForcibly()
            }
        } catch (_: InterruptedException) {
            timedOut = true
            process.destroyForcibly()
        }
        val stdout = readStream(process.inputStream)
        val stderr = readStream(process.errorStream)
        val exit = try {
            process.exitValue()
        } catch (_: IllegalThreadStateException) {
            -1
        }
        return ShellResult(exit, stdout, stderr, timedOut)
    }

    private fun readStream(stream: java.io.InputStream): String = try {
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
    } catch (_: Throwable) {
        ""
    }
}
