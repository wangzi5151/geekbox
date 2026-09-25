package com.tinyai.geekbox.core.util

import java.io.File

object SysFs {

    fun read(path: String): String? = try {
        val file = File(path)
        if (file.canRead()) file.readText().trim().ifBlank { null } else null
    } catch (_: Throwable) {
        null
    }

    fun readLong(path: String): Long? = read(path)?.takeWhile { it.isDigit() || it == '-' }?.toLongOrNull()

    fun listDirs(path: String): List<String> = try {
        File(path).listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    fun listFiles(path: String, filter: (String) -> Boolean = { true }): List<String> = try {
        File(path).listFiles()?.filter { it.isFile && filter(it.name) }?.map { it.name }?.sorted() ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}
