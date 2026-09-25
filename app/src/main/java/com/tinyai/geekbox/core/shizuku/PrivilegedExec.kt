package com.tinyai.geekbox.core.shizuku

import com.tinyai.geekbox.core.util.Shell
import com.tinyai.geekbox.core.util.ShellResult

object PrivilegedExec {

    fun available(): Boolean = ShizukuBridge.isAuthorized() || Shell.hasSu()

    fun modeLabel(): String = when {
        ShizukuBridge.isAuthorized() -> "Shizuku (shell)"
        Shell.hasSu() -> "Root"
        else -> "本地"
    }

    fun run(command: String, timeoutSeconds: Long = 30): ShellResult = when {
        ShizukuBridge.isAuthorized() -> ShizukuBridge.runShell(command, timeoutSeconds)
        Shell.hasSu() -> Shell.runRoot(command, timeoutSeconds)
        else -> Shell.run(command, timeoutSeconds)
    }
}
