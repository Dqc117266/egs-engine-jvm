package com.dqc.egsengine.feature.base.util

import java.io.File

object ProjectRootResolver {

    fun resolve(path: String): File {
        val start = File(path).let { if (it.isAbsolute) it else it.absoluteFile }
        return findProjectRoot(start)
            ?: throw IllegalArgumentException(
                "Could not find a Gradle project root from: ${start.absolutePath}\n" +
                    "Hint: use an absolute path, e.g. -p /path/to/project",
            )
    }

    private fun findProjectRoot(dir: File): File? {
        var current = dir.canonicalFile
        while (true) {
            if (isGradleProjectRoot(current)) return current
            current = current.parentFile ?: return null
        }
    }

    private fun isGradleProjectRoot(dir: File): Boolean =
        dir.resolve("settings.gradle.kts").exists() ||
            dir.resolve("settings.gradle").exists()
}
