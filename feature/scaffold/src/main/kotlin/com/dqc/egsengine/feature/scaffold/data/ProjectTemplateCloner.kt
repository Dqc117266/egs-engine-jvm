package com.dqc.egsengine.feature.scaffold.data

import java.io.File

internal class ProjectTemplateCloner(
    private val githubToken: String?,
    private val githubUsername: String,
    private val logInfo: (String) -> Unit,
) {

    fun cloneAndCustomize(canonicalUrl: String, targetDir: File, projectName: String) {
        val cloneUrl = GitHubCloneUrlPolicy.embedHttpsToken(canonicalUrl, githubToken, githubUsername)
        logInfo.invoke("Cloning template: $canonicalUrl")

        val parentDir = targetDir.parentFile ?: File(".")
        val result = exec(
            listOf("git", "clone", "--depth", "1", cloneUrl, targetDir.absolutePath),
            parentDir,
        )
        if (result.exitCode != 0) {
            throw IllegalStateException(
                "Template clone failed: ${result.output.ifBlank { "exitCode=${result.exitCode}" }}",
            )
        }

        targetDir.resolve(".git").takeIf { it.exists() }?.deleteRecursively()
        applyProjectNameToClonedTree(targetDir, projectName)
    }

    private fun applyProjectNameToClonedTree(root: File, projectName: String) {
        root.walkTopDown()
            .maxDepth(5)
            .filter { it.isFile && it.name == "settings.gradle.kts" }
            .forEach { patchRootProjectName(it, projectName) }

        root.walkTopDown()
            .maxDepth(8)
            .filter { it.isFile && it.name == "application.yml" }
            .forEach { patchSpringApplicationName(it, projectName) }

        root.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile && it.name == "package.json" }
            .forEach { patchPackageJsonName(it, projectName) }
    }

    private fun patchRootProjectName(file: File, projectName: String) {
        val text = file.readText()
        val replaced = ROOT_PROJECT_NAME_REGEX.replace(text) {
            """rootProject.name = "$projectName""""
        }
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun patchSpringApplicationName(file: File, projectName: String) {
        val text = file.readText()
        val replaced = SPRING_APP_NAME_BLOCK_REGEX.replace(text) { mr ->
            "${mr.groupValues[1]}$projectName"
        }
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun patchPackageJsonName(file: File, projectName: String) {
        val npmName = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')
            .ifBlank { "app" }
        val text = file.readText()
        val replaced = PACKAGE_JSON_NAME_REGEX.replace(text, """"name": "$npmName"""")
        if (text != replaced) {
            file.writeText(replaced)
        }
    }

    private fun exec(command: List<String>, workDir: File): ProcessResult {
        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        return ProcessResult(exitCode, output)
    }

    private data class ProcessResult(val exitCode: Int, val output: String)

    companion object {
        private val ROOT_PROJECT_NAME_REGEX = Regex("""rootProject\.name\s*=\s*"[^"]*"""")
        private val SPRING_APP_NAME_BLOCK_REGEX = Regex(
            """(spring:\s*\r?\n\s*application:\s*\r?\n\s*name:\s*)(\S+)""",
        )
        private val PACKAGE_JSON_NAME_REGEX = Regex(""""name"\s*:\s*"[^"]*"""")
    }
}
