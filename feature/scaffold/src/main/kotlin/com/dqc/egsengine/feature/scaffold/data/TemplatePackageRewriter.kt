package com.dqc.egsengine.feature.scaffold.data

import java.io.File
import java.nio.charset.MalformedInputException

/**
 * Recipe for renaming a cloned template's package / project-name tokens.
 * Any field left null is skipped during rewriting.
 *
 * 合并自原 data/ 与 data/template/ 两套实现（A-01）。
 */
data class TemplateRenameRecipe(
    /** 识别 id（android/kmp/server/admin），用于日志显示。 */
    val id: String? = null,
    /** Old base package, e.g. `com.example.egs_android_template`. Null skips package rewriting (e.g. ADMIN). */
    val oldPackage: String? = null,
    /** Old kebab-case project name, e.g. `egs-android-template`. */
    val oldProjectName: String? = null,
    /** Old display name (PascalCase / mixed case), e.g. `EGS-Android-Template`. */
    val oldProjectNameDisplay: String? = null,
    /** Old package leaf token (last segment of [oldPackage]), e.g. `egs_android_template`. */
    val oldPackageToken: String? = null,
    /**
     * Additional old packages that should also be rewritten (directory relocation + text replacement).
     * Each pair is (oldPackage, newPackageSuffix) — the newPackageSuffix is appended to the base
     * [newPackage] passed to [TemplatePackageRewriter.rewrite].
     */
    val extraOldPackages: Map<String, String> = emptyMap(),
)

/**
 * Renames package directories and rewrites textual references inside a cloned template.
 *
 * 合并自原 data/（forward + extraOldPackages + dotted matches）与 data/template/（forward/reverse + SKIP_DIR）两套实现。
 * 共享引擎由 [rewrite] / [rewriteReverse] 两个薄 API 暴露。
 */
class TemplatePackageRewriter {
    /**
     * Forward rewrite: apply [recipe] to [projectDir], renaming to [newProjectName] / [newPackage].
     * Used by `create project` / `new project` after cloning.
     */
    fun rewrite(
        projectDir: File,
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
    ) {
        recipe.oldPackage?.let { oldPkg ->
            relocatePackageDirectories(projectDir, oldPkg, newPackage)
            recipe.extraOldPackages.forEach { (extraOld, newSuffix) ->
                val fullNewPkg = if (newSuffix.isBlank()) newPackage else "$newPackage.$newSuffix"
                relocatePackageDirectories(projectDir, extraOld, fullNewPkg)
            }
        }

        rewriteTextFiles(projectDir, buildForwardReplacements(recipe, newProjectName, newPackage))
    }

    /**
     * Forward rewrite — alias of [rewrite] for call-site clarity (kept for the `create project` path).
     */
    fun rewriteForward(
        projectDir: File,
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
    ) = rewrite(projectDir, recipe, newProjectName, newPackage)

    /**
     * Reverse rewrite: rewrite text first, then relocate package dirs.
     * Used by template sync-back (rewriting copied files back to the template's canonical tokens).
     */
    fun rewriteReverse(
        projectDir: File,
        recipe: TemplateRenameRecipe,
        fromProjectName: String,
        fromPackage: String?,
        toProjectName: String,
        toPackage: String?,
    ) {
        rewriteTextFiles(
            projectDir,
            buildReverseReplacements(recipe, fromProjectName, fromPackage, toProjectName, toPackage),
        )
        if (toPackage != null && fromPackage != null && fromPackage != toPackage) {
            relocatePackageDirectories(projectDir, fromPackage, toPackage)
        }
    }

    private fun buildForwardReplacements(
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
    ): Map<String, String> {
        val newPackageToken = newPackage.substringAfterLast('.')
        val replacements = linkedMapOf<String, String>()
        recipe.oldPackage?.let { oldPkg ->
            replacements[oldPkg] = newPackage
            replacements[oldPkg.replace('.', '/')] = newPackage.replace('.', '/')
        }
        recipe.oldProjectName?.let { replacements[it] = newProjectName }
        recipe.oldProjectNameDisplay?.let { replacements[it] = newProjectName }
        recipe.oldPackageToken?.let { replacements[it] = newPackageToken }
        recipe.extraOldPackages.forEach { (extraOld, newSuffix) ->
            val fullNewPkg = if (newSuffix.isBlank()) newPackage else "$newPackage.$newSuffix"
            replacements[extraOld] = fullNewPkg
            replacements[extraOld.replace('.', '/')] = fullNewPkg.replace('.', '/')
        }
        return replacements
    }

    private fun buildReverseReplacements(
        recipe: TemplateRenameRecipe,
        fromProjectName: String,
        fromPackage: String?,
        toProjectName: String,
        toPackage: String?,
    ): Map<String, String> {
        val replacements = linkedMapOf<String, String>()
        if (fromPackage != null && toPackage != null && fromPackage != toPackage) {
            replacements[fromPackage] = toPackage
            replacements[fromPackage.replace('.', '/')] = toPackage.replace('.', '/')
            val fromToken = fromPackage.substringAfterLast('.')
            val toToken = toPackage.substringAfterLast('.')
            if (fromToken != toToken) {
                replacements[fromToken] = toToken
            }
        }
        if (fromProjectName != toProjectName) {
            replacements[fromProjectName] = toProjectName
        }
        recipe.oldProjectNameDisplay?.let { display ->
            if (fromProjectName != toProjectName && display != toProjectName) {
                replacements[fromProjectName] = toProjectName
            }
        }
        return replacements.filter { (from, to) -> from.isNotEmpty() && from != to }
    }

    private fun rewriteTextFiles(
        projectDir: File,
        replacements: Map<String, String>,
    ) {
        if (replacements.isEmpty()) return

        projectDir
            .walkTopDown()
            .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
            .filter { it.isFile && isLikelyTextFile(it) }
            .forEach { file ->
                val original = file.readUtf8TextOrNull() ?: return@forEach
                var updated = original
                replacements.forEach { (from, to) ->
                    if (from.isNotEmpty() && from != to) {
                        updated = updated.replace(from, to)
                    }
                }
                if (updated != original) {
                    file.writeText(updated)
                }
            }
    }

    private fun isLikelyTextFile(file: File): Boolean {
        if (file.extension.lowercase() in BINARY_EXTENSIONS) return false

        val bytes =
            file.inputStream().use { input ->
                val preview = ByteArray(8192)
                val readSize = input.read(preview)
                if (readSize <= 0) return true
                preview.copyOf(readSize)
            }

        if (bytes.any { it == 0.toByte() }) return false

        val controlChars =
            bytes.count {
                val value = it.toInt() and 0xFF
                value < 0x09 || (value in 0x0E..0x1F)
            }
        return controlChars < bytes.size / 3
    }

    private fun File.readUtf8TextOrNull(): String? = try {
        readText()
    } catch (_: MalformedInputException) {
        null
    }

    private fun relocatePackageDirectories(
        projectDir: File,
        oldPackage: String,
        newPackage: String,
    ) {
        val oldPackagePath = oldPackage.replace('.', '/')
        val newPackagePath = newPackage.replace('.', '/')
        if (oldPackagePath == newPackagePath) return

        // Match standard nested directories (e.g. template/core/base/analytics)
        val nestedMatches =
            projectDir
                .walkTopDown()
                .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                .filter { it.isDirectory }
                .filter { directory ->
                    val relativePath = directory.relativeTo(projectDir).path.replace(File.separatorChar, '/')
                    relativePath.endsWith(oldPackagePath)
                }.toList()

        // Match dot-separated directory names (e.g. template.core.base.analytics as a single dir)
        val dottedMatches =
            if (oldPackage.contains('.')) {
                projectDir
                    .walkTopDown()
                    .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                    .filter { it.isDirectory }
                    .filter { directory ->
                        directory.name == oldPackage || directory.name.startsWith(oldPackage + ".")
                    }.toList()
            } else {
                emptyList()
            }

        val packageDirectories =
            (nestedMatches + dottedMatches)
                .distinctBy { it.absolutePath }
                .sortedByDescending { it.absolutePath.length }

        packageDirectories.forEach { sourceDir ->
            if (!sourceDir.exists()) return@forEach

            val relativePath = sourceDir.relativeTo(projectDir).path.replace(File.separatorChar, '/')
            val isDottedMatch = sourceDir.name == oldPackage || sourceDir.name.startsWith(oldPackage + ".")
            val suffix =
                when {
                    isDottedMatch -> sourceDir.name
                    relativePath.endsWith(oldPackagePath) -> oldPackagePath
                    relativePath.endsWith(oldPackage) -> oldPackage
                    else -> oldPackagePath
                }
            val dottedSuffixExtra =
                if (isDottedMatch && sourceDir.name != oldPackage) {
                    sourceDir.name.removePrefix(oldPackage)
                } else {
                    ""
                }
            val targetPkgPath =
                if (dottedSuffixExtra.isNotEmpty()) {
                    newPackagePath + dottedSuffixExtra.replace('.', '/')
                } else {
                    newPackagePath
                }
            val prefixPath = relativePath.removeSuffix(suffix).trimEnd('/')
            val targetRelativePath =
                buildString {
                    if (prefixPath.isNotEmpty()) {
                        append(prefixPath)
                        append('/')
                    }
                    append(targetPkgPath)
                }

            val targetDir = projectDir.resolve(targetRelativePath)
            if (sourceDir.absolutePath == targetDir.absolutePath) return@forEach

            moveDirectoryWithMerge(sourceDir, targetDir)
            cleanupEmptyDirectories(sourceDir.parentFile, projectDir)
        }
    }

    private fun moveDirectoryWithMerge(
        source: File,
        target: File,
    ) {
        if (!target.exists()) {
            target.parentFile?.mkdirs()
            if (source.renameTo(target)) return
        }

        target.mkdirs()
        source.listFiles().orEmpty().forEach { child ->
            val destination = target.resolve(child.name)
            if (child.isDirectory) {
                moveDirectoryWithMerge(child, destination)
            } else {
                destination.parentFile?.mkdirs()
                if (destination.exists()) {
                    destination.delete()
                }
                if (!child.renameTo(destination)) {
                    child.copyTo(destination, overwrite = true)
                    child.delete()
                }
            }
        }

        if (source.exists()) {
            source.deleteRecursively()
        }
    }

    private fun cleanupEmptyDirectories(
        start: File?,
        stopAt: File,
    ) {
        var current = start
        while (current != null && current.absolutePath != stopAt.absolutePath) {
            if (!current.exists() || !current.isDirectory || !current.listFiles().isNullOrEmpty()) {
                break
            }
            val parent = current.parentFile
            current.delete()
            current = parent
        }
    }

    private companion object {
        val SKIP_DIR_NAMES = setOf(".git", "build", ".gradle", "node_modules", ".idea")

        val BINARY_EXTENSIONS =
            setOf(
                "png",
                "jpg",
                "jpeg",
                "gif",
                "webp",
                "jar",
                "zip",
                "ico",
                "keystore",
                "jks",
                "ttf",
                "otf",
                "so",
                "pdf",
                "mp3",
                "mp4",
                "wav",
            )
    }
}

/**
 * Built-in rename recipes keyed by template canonical URL / path (scheme/auth-agnostic match).
 * 合并自原 data/ 与 data/template/ 两套 recipes。
 */
internal object TemplateRenameRecipes {
    /** Shared instance for the default Android client template. */
    val ANDROID_CLIENT: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "android",
            oldPackage = "com.example.egs_android_template",
            oldProjectName = "egs-android-template",
            oldProjectNameDisplay = "EGS-Android-Template",
            oldPackageToken = "egs_android_template",
            extraOldPackages =
            mapOf(
                "org.mifos" to "",
                "template.core.base" to "core.base",
            ),
        )

    /** Recipe for the KMP (Compose Multiplatform) client template. */
    val KMP_CLIENT: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "kmp",
            oldPackage = "org.mifos",
            oldProjectName = "egs-kmp-template",
            oldProjectNameDisplay = "egs-kmp-template",
            extraOldPackages =
            mapOf(
                "template.core.base" to "core.base",
                "cmp.android.app" to "cmp.android.app",
                "cmp.navigation" to "cmp.navigation",
                "cmp.shared" to "cmp.shared",
            ),
        )

    /** Recipe for the Spring Boot backend template. */
    val BACKEND: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "server",
            oldPackage = "com.egs.server",
            oldProjectName = "egs-server-template",
        )

    /** Recipe for the Vue3 admin template (no package dir, only project-name tokens). */
    val ADMIN: TemplateRenameRecipe =
        TemplateRenameRecipe(
            id = "admin",
            oldProjectName = "egs-admin-template",
            oldProjectNameDisplay = "egs-admin-template",
        )

    /**
     * Returns a recipe for the given canonical template URL, or null when no rewriting recipe is
     * registered for that template.
     */
    fun recipeFor(canonicalUrl: String?): TemplateRenameRecipe? {
        val url = canonicalUrl?.lowercase() ?: return null
        return detectFromPath(url)
    }

    /** Detect a recipe from a local path / URL (case-insensitive, path-separator normalized). */
    fun detectFromPath(path: String): TemplateRenameRecipe? {
        val normalized = path.lowercase().replace('\\', '/')
        return when {
            "egs-android-template" in normalized -> ANDROID_CLIENT
            "egs-kmp-template" in normalized -> KMP_CLIENT
            "egs-server-template" in normalized -> BACKEND
            "egs-admin-template" in normalized -> ADMIN
            else -> null
        }
    }
}
