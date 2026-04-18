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

    /**
     * Prefer walking from the **absolute** path (symlinks not resolved) so that a workspace
     * reached through a symlink — e.g. `.../egs-group/android-test` -> `.../.Trashes/...` —
     * resolves to the path the user actually `cd`'d into, and generated files show up where the
     * IDE project is opened. Using [File.getCanonicalFile] first used to anchor the walk in
     * `.Trashes/...`, which made all writes go to the trash copy and looked like "no code under
     * feature/" in the normal project folder.
     */
    private fun findProjectRoot(start: File): File? {
        val absoluteStart = start.absoluteFile
        findProjectRootWalkingUp(absoluteStart)?.let { return it }

        val canonicalStart = try {
            absoluteStart.canonicalFile
        } catch (_: Exception) {
            null
        }
        if (canonicalStart != null && canonicalStart != absoluteStart) {
            findProjectRootWalkingUp(canonicalStart)?.let { return it }
        }
        return null
    }

    private fun findProjectRootWalkingUp(start: File): File? {
        var current: File? = start
        while (current != null) {
            if (isWorkspaceRoot(current)) return current
            current = current.parentFile
        }
        current = start
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

    /**
     * Resolves the Gradle subproject root that contains `feature/<name>` modules.
     *
     * Multi-repo EGS workspaces keep the KMP/Android app under `client/` while the
     * workspace root only has `.egs/workspace.json`. Single-repo projects place
     * `feature/` directly under the Gradle root.
     */
    fun resolveGradleClientRoot(resolvedRoot: File): File {
        val client = resolvedRoot.resolve("client")
        val clientFeature = client.resolve("feature")
        val rootFeature = resolvedRoot.resolve("feature")
        return when {
            clientFeature.isDirectory -> client
            rootFeature.isDirectory -> resolvedRoot
            client.isDirectory &&
                (client.resolve("settings.gradle.kts").exists() || client.resolve("settings.gradle").exists()) -> client
            else -> resolvedRoot
        }
    }
}
