package com.dqc.egsengine.feature.init.data

import java.io.File

/**
 * Resolves Kotlin/Java source roots under a Gradle module (`src/<sourceSet>/kotlin`, etc.),
 * including KMP sets such as `commonMain` and Android `main`.
 */
object GradleSourceRoots {

    fun kotlinRoots(moduleDir: File): List<File> {
        val src = moduleDir.resolve("src")
        if (!src.isDirectory) return emptyList()
        return src.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { sourceSetDir -> sourceSetDir.resolve("kotlin").takeIf { it.isDirectory } }
            ?: emptyList()
    }

    fun javaRoots(moduleDir: File): List<File> {
        val src = moduleDir.resolve("src")
        if (!src.isDirectory) return emptyList()
        return src.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { sourceSetDir -> sourceSetDir.resolve("java").takeIf { it.isDirectory } }
            ?: emptyList()
    }

    fun orderedJavaRoots(moduleDir: File): List<File> =
        javaRoots(moduleDir).sortedWith(
            compareBy<File> { sourceSetSortKey(it.parentFile.name) }
                .thenBy { it.path },
        )

    /**
     * Prefer [commonMain], then [main], then stable path order — so scans and heuristics
     * pick shared KMP code before platform-specific duplicates.
     */
    fun orderedKotlinRoots(moduleDir: File): List<File> =
        kotlinRoots(moduleDir).sortedWith(
            compareBy<File> { sourceSetSortKey(it.parentFile.name) }
                .thenBy { it.path },
        )

    private fun sourceSetSortKey(name: String): Int =
        when (name) {
            "commonMain" -> 0
            "main" -> 1
            else -> 2
        }

    fun hasAndroidStyleRes(moduleDir: File): Boolean {
        val src = moduleDir.resolve("src")
        if (!src.isDirectory) return false
        return src.listFiles()?.any { child ->
            child.isDirectory && child.resolve("res").isDirectory
        } ?: false
    }
}
