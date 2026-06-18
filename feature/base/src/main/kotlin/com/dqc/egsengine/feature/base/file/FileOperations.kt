package com.dqc.egsengine.feature.base.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileOperations {
    suspend fun writeText(
        file: File,
        text: String,
    ) = withContext(Dispatchers.IO) {
        file.parentFile?.takeIf { !it.exists() }?.mkdirs()
        file.writeText(text)
    }

    fun writeTextAtomic(
        file: File,
        text: String,
    ) {
        file.parentFile?.takeIf { !it.exists() }?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    suspend fun readText(file: File): String = withContext(Dispatchers.IO) { file.readText() }

    fun readTextSync(file: File): String = file.readText()

    fun ensureWithin(
        root: File,
        path: File,
    ) {
        val rootCanonical = root.canonicalPath
        val pathCanonical = path.canonicalPath
        require(pathCanonical.startsWith(rootCanonical)) {
            "Path '$path' is outside allowed root '$root'"
        }
    }
}
