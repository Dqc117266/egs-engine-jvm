package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.base.command.CommandExecutor
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Base64

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
        logInfo.invoke("Cloning template: $canonicalUrl")

        val parentDir = targetDir.parentFile ?: File(".")
        val env = buildCloneEnvironment(canonicalUrl)
        val result =
            exec(
                listOf("git", "clone", "--depth", "1", canonicalUrl, targetDir.absolutePath),
                parentDir,
                env,
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

    /**
     * Build environment for git clone. When a GitHub token is available and the URL is HTTPS,
     * pass credentials via GIT_HTTP_EXTRAHEADER to avoid leaking tokens in argv/ps.
     */
    private fun buildCloneEnvironment(url: String): Map<String, String> {
        val token = githubToken?.trim()?.takeIf { it.isNotBlank() } ?: return emptyMap()
        if (!url.startsWith("https://github.com/", ignoreCase = true)) return emptyMap()

        val user = githubUsername.trim().ifBlank { "x-access-token" }
        val raw = "$user:$token"
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray())
        return mapOf(
            "GIT_TERMINAL_PROMPT" to "0",
            "GIT_HTTP_EXTRAHEADER" to "Authorization: Basic $encoded",
        )
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
        environment: Map<String, String> = emptyMap(),
    ): ProcessResult {
        val result = runBlocking { commandExecutor.execute(command, workDir, environment) }
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
