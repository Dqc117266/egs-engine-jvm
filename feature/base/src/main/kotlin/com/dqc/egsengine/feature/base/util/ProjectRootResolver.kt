package com.dqc.egsengine.feature.base.util

import java.io.File

/**
 * Resolves the directory EGS commands should treat as "project root".
 *
 * Order (nearest ancestor from [start] upward):
 * 1. A directory containing **`.egs/workspace.json`** (multi-project workspace from `egs-engine new project`).
 *    That folder usually has **no** `settings.gradle` at the root; Gradle roots live under `client/`, `backend/`, etc.
 * 2. A directory containing **`settings.gradle.kts`** or **`settings.gradle`** (single Gradle project or subfolder inside one).
 */
object ProjectRootResolver {

    fun resolve(path: String): File {
        val start = File(path).let { if (it.isAbsolute) it else it.absoluteFile }
        return findProjectRoot(start)
            ?: throw IllegalArgumentException(
                "Could not find an EGS workspace or Gradle project root from: ${start.absolutePath}\n" +
                    "Hint: `cd` into the workspace folder that contains `.egs/workspace.json`, " +
                    "or into a Gradle project (with settings.gradle), " +
                    "or pass an absolute path: -p /path/to/workspace",
            )
    }

    fun hasGradleSettings(dir: File): Boolean = isGradleProjectRoot(dir)

    private fun findProjectRoot(start: File): File? {
        val canonicalStart = try {
            start.canonicalFile
        } catch (_: Exception) {
            start.absoluteFile
        }
        // 1) Nearest ancestor with workspace.json (multi-project root)
        var current: File? = canonicalStart
        while (current != null) {
            if (isWorkspaceRoot(current)) return current
            current = current.parentFile
        }
        // 2) Nearest ancestor with Gradle settings
        current = canonicalStart
        while (current != null) {
            if (isGradleProjectRoot(current)) return current
            current = current.parentFile
        }
        return null
    }

    private fun isWorkspaceRoot(dir: File): Boolean =
        dir.resolve(".egs/workspace.json").exists()

    private fun isGradleProjectRoot(dir: File): Boolean =
        dir.resolve("settings.gradle.kts").exists() ||
            dir.resolve("settings.gradle").exists()
}
