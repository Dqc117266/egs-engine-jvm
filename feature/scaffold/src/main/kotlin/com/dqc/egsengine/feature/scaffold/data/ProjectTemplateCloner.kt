package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.base.command.CommandExecutor
import kotlinx.coroutines.runBlocking
import java.io.File

internal class ProjectTemplateCloner(
    private val githubToken: String?,
    private val githubUsername: String,
    private val commandExecutor: CommandExecutor = CommandExecutor(),
    private val logInfo: (String) -> Unit = {},
) {
    private val packageRewriter = TemplatePackageRewriter()

    /**
     * Clones [canonicalUrl] into [targetDir], then applies project-name patches and (optionally)
     * the full package / project-name rewrite recipe for this template.
     *
     * @param packageName when non-null, will be used as the new base package in [TemplateRenameRecipes].
     * If no recipe is registered for [canonicalUrl], the package rewrite is skipped silently.
     */
    fun cloneAndCustomize(
        canonicalUrl: String,
        targetDir: File,
        projectName: String,
        packageName: String? = null,
    ) {
        val cloneUrl = GitHubCloneUrlPolicy.embedHttpsToken(canonicalUrl, githubToken, githubUsername)
        logInfo.invoke("Cloning template: $canonicalUrl")

        val parentDir = targetDir.parentFile ?: File(".")
        val result =
            exec(
                listOf("git", "clone", "--depth", "1", cloneUrl, targetDir.absolutePath),
                parentDir,
            )
        if (result.exitCode != 0) {
            error(
                "Template clone failed: ${result.output.ifBlank { "exitCode=${result.exitCode}" }}",
            )
        }

        targetDir.resolve(".git").takeIf { it.exists() }?.deleteRecursively()
        applyProjectNameToClonedTree(targetDir, projectName)

        if (packageName != null) {
            TemplateRenameRecipes.recipeFor(canonicalUrl)?.let { recipe ->
                logInfo.invoke(
                    "Rewriting package ${recipe.oldPackage} -> $packageName in ${targetDir.name}",
                )
                packageRewriter.rewrite(
                    projectDir = targetDir,
                    recipe = recipe,
                    newProjectName = projectName,
                    newPackage = packageName,
                )
            }
        }
    }

    private fun applyProjectNameToClonedTree(
        root: File,
        projectName: String,
    ) {
        root
            .walkTopDown()
            .maxDepth(5)
            .filter { it.isFile && it.name == "settings.gradle.kts" }
            .forEach { patchRootProjectName(it, projectName) }

        root
            .walkTopDown()
            .maxDepth(8)
            .filter { it.isFile && it.name == "application.yml" }
            .forEach { patchSpringApplicationName(it, projectName) }

        root
            .walkTopDown()
            .maxDepth(4)
            .filter { it.isFile && it.name == "package.json" }
            .forEach { patchPackageJsonName(it, projectName) }
    }

    private fun patchRootProjectName(
        file: File,
        projectName: String,
    ) {
        val text = file.readText()
        val replaced =
            ROOT_PROJECT_NAME_REGEX.replace(text) {
                """rootProject.name = "$projectName""""
            }
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun patchSpringApplicationName(
        file: File,
        projectName: String,
    ) {
        val text = file.readText()
        val replaced =
            SPRING_APP_NAME_BLOCK_REGEX.replace(text) { mr ->
                "${mr.groupValues[1]}$projectName"
            }
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun patchPackageJsonName(
        file: File,
        projectName: String,
    ) {
        val npmName =
            projectName
                .lowercase()
                .replace(Regex("[^a-z0-9-]"), "-")
                .trim('-')
                .ifBlank { "app" }
        val text = file.readText()
        val replaced = PACKAGE_JSON_NAME_REGEX.replace(text, """"name": "$npmName"""")
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun exec(
        command: List<String>,
        workDir: File,
    ): ProcessResult {
        // 统一走 CommandExecutor（P0：分开消费 stdout/stderr，避免大输出死锁）。
        val result = runBlocking { commandExecutor.execute(command, workDir) }
        val combined = listOf(result.output, result.error).filter { it.isNotBlank() }.joinToString("\n")
        return ProcessResult(result.exitCode, combined)
    }

    private data class ProcessResult(
        val exitCode: Int,
        val output: String,
    )

    companion object {
        private val ROOT_PROJECT_NAME_REGEX = Regex("""rootProject\.name\s*=\s*"[^"]*"""")
        private val SPRING_APP_NAME_BLOCK_REGEX =
            Regex(
                """(spring:\s*\r?\n\s*application:\s*\r?\n\s*name:\s*)(\S+)""",
            )
        private val PACKAGE_JSON_NAME_REGEX = Regex(""""name"\s*:\s*"[^"]*"""")
    }
}
