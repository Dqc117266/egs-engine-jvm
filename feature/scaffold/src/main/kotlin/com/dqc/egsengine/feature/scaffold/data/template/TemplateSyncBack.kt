package com.dqc.egsengine.feature.scaffold.data.template

import java.io.File

data class TemplateSyncBackResult(
    val dryRun: Boolean,
    val copiedFiles: List<String>,
    val rewrittenFiles: List<String>,
)

class TemplateSyncBack(
    private val packageRewriter: TemplatePackageRewriter = TemplatePackageRewriter(),
) {
    fun sync(
        fromDir: File,
        toDir: File,
        recipe: TemplateRenameRecipe,
        fromProjectName: String,
        fromPackage: String?,
        toProjectName: String,
        toPackage: String?,
        pathFilters: List<String> = emptyList(),
        dryRun: Boolean = false,
    ): TemplateSyncBackResult {
        require(fromDir.isDirectory) { "Source is not a directory: ${fromDir.absolutePath}" }
        require(toDir.isDirectory) { "Target is not a directory: ${toDir.absolutePath}" }

        val relativePaths = resolveRelativePaths(fromDir, pathFilters)
        val copiedFiles = mutableListOf<String>()

        relativePaths.forEach { relativePath ->
            val source = fromDir.resolve(relativePath)
            if (!source.exists()) return@forEach

            val target = toDir.resolve(relativePath)
            if (dryRun) {
                if (source.isDirectory) {
                    source
                        .walkTopDown()
                        .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                        .filter { it.isFile }
                        .forEach { file ->
                            val rel = file.relativeTo(fromDir).path.replace(File.separatorChar, '/')
                            copiedFiles.add(rel)
                        }
                } else {
                    copiedFiles.add(relativePath)
                }
            } else {
                copyPath(source, target, copiedFiles, fromDir, toDir)
            }
        }

        val rewrittenFiles =
            if (dryRun) {
                emptyList()
            } else {
                val before = snapshotTextFiles(toDir, relativePaths)
                packageRewriter.rewriteReverse(
                    projectDir = toDir,
                    recipe = recipe,
                    fromProjectName = fromProjectName,
                    fromPackage = fromPackage,
                    toProjectName = toProjectName,
                    toPackage = toPackage,
                )
                diffTextFiles(toDir, relativePaths, before)
            }

        return TemplateSyncBackResult(
            dryRun = dryRun,
            copiedFiles = copiedFiles.distinct().sorted(),
            rewrittenFiles = rewrittenFiles.sorted(),
        )
    }

    private fun resolveRelativePaths(
        fromDir: File,
        pathFilters: List<String>,
    ): List<String> = if (pathFilters.isEmpty()) {
        fromDir
            .listFiles()
            .orEmpty()
            .filter { it.name !in DEFAULT_SKIP_TOP_LEVEL }
            .map { it.name }
    } else {
        pathFilters.map { it.trim().trim('/') }.filter { it.isNotEmpty() }
    }

    private fun copyPath(
        source: File,
        target: File,
        copiedFiles: MutableList<String>,
        fromRoot: File,
        toRoot: File,
    ) {
        if (source.isDirectory) {
            source
                .walkTopDown()
                .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                .filter { it.isFile }
                .forEach { file ->
                    val relToFrom = file.relativeTo(fromRoot).path.replace(File.separatorChar, '/')
                    val dest = toRoot.resolve(relToFrom)
                    dest.parentFile?.mkdirs()
                    file.copyTo(dest, overwrite = true)
                    copiedFiles.add(relToFrom)
                }
        } else {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            copiedFiles.add(source.relativeTo(fromRoot).path.replace(File.separatorChar, '/'))
        }
    }

    private fun snapshotTextFiles(
        root: File,
        relativePaths: List<String>,
    ): Map<String, String> {
        val snapshot = linkedMapOf<String, String>()
        relativePaths.forEach { relativePath ->
            val base = root.resolve(relativePath)
            if (!base.exists()) return@forEach
            if (base.isFile && isTrackableText(base)) {
                snapshot[relativePath] = base.readText()
                return@forEach
            }
            base
                .walkTopDown()
                .onEnter { dir -> dir.name !in SKIP_DIR_NAMES }
                .filter { it.isFile && isTrackableText(it) }
                .forEach { file ->
                    val rel = file.relativeTo(root).path.replace(File.separatorChar, '/')
                    snapshot[rel] = file.readText()
                }
        }
        return snapshot
    }

    private fun diffTextFiles(
        root: File,
        relativePaths: List<String>,
        before: Map<String, String>,
    ): List<String> {
        val after = snapshotTextFiles(root, relativePaths)
        return after.keys.filter { key -> before[key] != after[key] }
    }

    private fun isTrackableText(file: File): Boolean {
        if (file.extension.lowercase() in BINARY_EXTENSIONS) return false
        return true
    }

    companion object {
        private val SKIP_DIR_NAMES = setOf(".git", "build", ".gradle", "node_modules", ".idea", ".egs")
        private val DEFAULT_SKIP_TOP_LEVEL = setOf(".git", "build", ".gradle", "node_modules", ".idea", ".egs")
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
