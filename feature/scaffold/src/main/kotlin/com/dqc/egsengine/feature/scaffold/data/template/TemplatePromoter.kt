package com.dqc.egsengine.feature.scaffold.data.template

import java.io.File

data class TemplatePromoteResult(
    val dryRun: Boolean,
    val promotedFiles: List<String>,
    val targetRoot: File,
)

class TemplatePromoter {

    fun promote(
        sourceRoot: File,
        targetRoot: File,
        dryRun: Boolean = false,
    ): TemplatePromoteResult {
        require(sourceRoot.isDirectory) { "Source is not a directory: ${sourceRoot.absolutePath}" }
        require(targetRoot.isDirectory) { "Target is not a directory: ${targetRoot.absolutePath}" }

        val promoted = mutableListOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension.equals("ftl", ignoreCase = true) }
            .forEach { file ->
                val relative = file.relativeTo(sourceRoot).path.replace(File.separatorChar, '/')
                val target = targetRoot.resolve(relative)
                if (dryRun) {
                    promoted.add(relative)
                } else {
                    target.parentFile?.mkdirs()
                    file.copyTo(target, overwrite = true)
                    promoted.add(relative)
                }
            }

        return TemplatePromoteResult(
            dryRun = dryRun,
            promotedFiles = promoted.sorted(),
            targetRoot = targetRoot,
        )
    }

    companion object {
        const val ENV_TEMPLATE_ROOT = "EGS_TEMPLATE_ROOT"

        fun defaultBundledTemplateRoot(engineRoot: File): File =
            engineRoot.resolve("feature/template-engine/src/main/resources/templates")

        fun resolveTargetRoot(explicit: String?): File? {
            explicit?.trim()?.takeIf { it.isNotEmpty() }?.let { return File(it) }
            System.getenv(ENV_TEMPLATE_ROOT)?.trim()?.takeIf { it.isNotEmpty() }?.let { return File(it) }
            return null
        }
    }
}
