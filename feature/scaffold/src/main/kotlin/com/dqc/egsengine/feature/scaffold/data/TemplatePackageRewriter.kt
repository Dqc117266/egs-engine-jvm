package com.dqc.egsengine.feature.scaffold.data

import java.io.File
import java.nio.charset.MalformedInputException

/**
 * Recipe for renaming a cloned template's package / project-name tokens.
 * Any field left null is skipped during rewriting.
 */
data class TemplateRenameRecipe(
    /** Old base package, e.g. `com.example.egs_android_template`. */
    val oldPackage: String,
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
     * For example, `"template.core.base"` → `"core.base"` maps `template.core.base.preferences`
     * to `com.dqc.demo.core.base.preferences`.
     */
    val extraOldPackages: Map<String, String> = emptyMap(),
)

/**
 * Renames package directories and rewrites textual references inside a cloned template.
 *
 * Extracted from the original `create project` flow so that `new project` can apply the same
 * transformations.
 */
class TemplatePackageRewriter {

    /**
     * @param projectDir the cloned template directory to mutate in-place.
     * @param recipe pattern describing what textual / directory tokens to rewrite.
     * @param newProjectName new kebab-case project name (replaces [TemplateRenameRecipe.oldProjectName] / display).
     * @param newPackage new fully-qualified base package (replaces [TemplateRenameRecipe.oldPackage]).
     */
    fun rewrite(
        projectDir: File,
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
    ) {
        // Relocate primary package directories
        relocatePackageDirectories(
            projectDir = projectDir,
            oldPackage = recipe.oldPackage,
            newPackage = newPackage,
        )

        // Relocate extra package directories
        recipe.extraOldPackages.forEach { (oldPkg, newSuffix) ->
            val fullNewPkg = "$newPackage.$newSuffix"
            relocatePackageDirectories(
                projectDir = projectDir,
                oldPackage = oldPkg,
                newPackage = fullNewPkg,
            )
        }

        val newPackageToken = newPackage.substringAfterLast('.')
        val replacements = linkedMapOf<String, String>()
        replacements[recipe.oldPackage] = newPackage
        replacements[recipe.oldPackage.replace('.', '/')] = newPackage.replace('.', '/')
        recipe.oldProjectName?.let { replacements[it] = newProjectName }
        recipe.oldProjectNameDisplay?.let { replacements[it] = newProjectName }
        recipe.oldPackageToken?.let { replacements[it] = newPackageToken }

        // Extra package text replacements
        recipe.extraOldPackages.forEach { (oldPkg, newSuffix) ->
            val fullNewPkg = "$newPackage.$newSuffix"
            replacements[oldPkg] = fullNewPkg
            replacements[oldPkg.replace('.', '/')] = fullNewPkg.replace('.', '/')
        }

        rewriteTextFiles(projectDir, replacements)
    }

    private fun rewriteTextFiles(projectDir: File, replacements: Map<String, String>) {
        if (replacements.isEmpty()) return

        projectDir.walkTopDown()
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

        val bytes = file.inputStream().use { input ->
            val preview = ByteArray(8192)
            val readSize = input.read(preview)
            if (readSize <= 0) return true
            preview.copyOf(readSize)
        }

        if (bytes.any { it == 0.toByte() }) return false

        val controlChars = bytes.count {
            val value = it.toInt() and 0xFF
            value < 0x09 || (value in 0x0E..0x1F)
        }
        return controlChars < bytes.size / 3
    }

    private fun File.readUtf8TextOrNull(): String? =
        try {
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

        val packageDirectories = projectDir.walkTopDown()
            .filter { it.isDirectory }
            .filter { directory ->
                val relativePath = directory.relativeTo(projectDir).path.replace(File.separatorChar, '/')
                relativePath.endsWith(oldPackagePath)
            }
            .toList()
            .sortedByDescending { it.absolutePath.length }

        packageDirectories.forEach { sourceDir ->
            if (!sourceDir.exists()) return@forEach

            val relativePath = sourceDir.relativeTo(projectDir).path.replace(File.separatorChar, '/')
            val prefixPath = relativePath.removeSuffix(oldPackagePath).trimEnd('/')
            val targetRelativePath = buildString {
                if (prefixPath.isNotEmpty()) {
                    append(prefixPath)
                    append('/')
                }
                append(newPackagePath)
            }

            val targetDir = projectDir.resolve(targetRelativePath)
            if (sourceDir.absolutePath == targetDir.absolutePath) return@forEach

            moveDirectoryWithMerge(sourceDir, targetDir)
            cleanupEmptyDirectories(sourceDir.parentFile, projectDir)
        }
    }

    private fun moveDirectoryWithMerge(source: File, target: File) {
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

    private fun cleanupEmptyDirectories(start: File?, stopAt: File) {
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
        val BINARY_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "gif", "webp",
            "jar", "zip", "ico",
            "keystore", "jks",
            "ttf", "otf",
            "so", "pdf",
            "mp3", "mp4", "wav",
        )
    }
}

/**
 * Built-in rename recipes keyed by template canonical URL (scheme/auth-agnostic match).
 */
internal object TemplateRenameRecipes {

    /** Shared instance for the default Android client template. */
    val ANDROID_CLIENT: TemplateRenameRecipe = TemplateRenameRecipe(
        oldPackage = "com.example.egs_android_template",
        oldProjectName = "egs-android-template",
        oldProjectNameDisplay = "EGS-Android-Template",
        oldPackageToken = "egs_android_template",
    )

    /** Recipe for the KMP (Compose Multiplatform) client template. */
    val KMP_CLIENT: TemplateRenameRecipe = TemplateRenameRecipe(
        oldPackage = "org.mifos",
        oldProjectName = "egs-kmp-template",
        oldProjectNameDisplay = "egs-kmp-template",
        extraOldPackages = mapOf(
            "template.core.base" to "core.base",
        ),
    )

    /**
     * Returns a recipe for the given canonical template URL, or null when no rewriting recipe is
     * registered for that template.
     */
    fun recipeFor(canonicalUrl: String?): TemplateRenameRecipe? {
        val url = canonicalUrl?.lowercase() ?: return null
        return when {
            url.contains("egs-android-template") -> ANDROID_CLIENT
            url.contains("egs-kmp-template") -> KMP_CLIENT
            else -> null
        }
    }
}
