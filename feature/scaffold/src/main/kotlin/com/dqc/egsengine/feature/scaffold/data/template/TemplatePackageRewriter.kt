package com.dqc.egsengine.feature.scaffold.data.template

import java.io.File
import java.nio.charset.MalformedInputException

/**
 * Renames package directories and rewrites textual references inside a project tree.
 */
class TemplatePackageRewriter {
    fun rewriteForward(
        projectDir: File,
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
    ) {
        if (recipe.oldPackage != null) {
            relocatePackageDirectories(
                projectDir = projectDir,
                oldPackage = recipe.oldPackage,
                newPackage = newPackage,
            )
        }
        rewriteTextFiles(projectDir, buildReplacements(recipe, newProjectName, newPackage, forward = true))
    }

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
            relocatePackageDirectories(
                projectDir = projectDir,
                oldPackage = fromPackage,
                newPackage = toPackage,
            )
        }
    }

    private fun buildReplacements(
        recipe: TemplateRenameRecipe,
        newProjectName: String,
        newPackage: String,
        forward: Boolean,
    ): Map<String, String> {
        val newPackageToken = newPackage.substringAfterLast('.')
        val replacements = linkedMapOf<String, String>()
        recipe.oldPackage?.let { replacements[it] = newPackage }
        recipe.oldPackage?.let { replacements[it.replace('.', '/')] = newPackage.replace('.', '/') }
        recipe.oldProjectName?.let { replacements[it] = newProjectName }
        recipe.oldProjectNameDisplay?.let { replacements[it] = newProjectName }
        recipe.oldPackageToken?.let { replacements[it] = newPackageToken }
        return if (forward) replacements else replacements.entries.associate { (k, v) -> v to k }
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

    private fun relocatePackageDirectories(
        projectDir: File,
        oldPackage: String,
        newPackage: String,
    ) {
        val oldPackagePath = oldPackage.replace('.', '/')
        val newPackagePath = newPackage.replace('.', '/')
        if (oldPackagePath == newPackagePath) return

        val packageDirectories =
            projectDir
                .walkTopDown()
                .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                .filter { it.isDirectory }
                .filter { directory ->
                    val relativePath = directory.relativeTo(projectDir).path.replace(File.separatorChar, '/')
                    relativePath.endsWith(oldPackagePath)
                }.toList()
                .sortedByDescending { it.absolutePath.length }

        packageDirectories.forEach { sourceDir ->
            if (!sourceDir.exists()) return@forEach

            val relativePath = sourceDir.relativeTo(projectDir).path.replace(File.separatorChar, '/')
            val prefixPath = relativePath.removeSuffix(oldPackagePath).trimEnd('/')
            val targetRelativePath =
                buildString {
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

    companion object {
        private val SKIP_DIR_NAMES = setOf(".git", "build", ".gradle", "node_modules", ".idea")

        private val BINARY_EXTENSIONS =
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
