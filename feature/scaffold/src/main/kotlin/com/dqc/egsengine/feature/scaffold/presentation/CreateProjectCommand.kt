package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.init.domain.ProjectInitializer
import com.dqc.egsengine.feature.scaffold.data.template.TemplatePackageRewriter
import com.dqc.egsengine.feature.scaffold.data.template.TemplateRenameRecipes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Base64

class CreateProjectCommand : CliktCommand(name = "project"), KoinComponent {

    private val initializer: ProjectInitializer by inject()

    private val projectNameArg by argument(help = "Project name").optional()

    private val packageNameOption by option(
        "--package",
        help = "Base package name, e.g. com.dqc.workflow",
    )

    private val typeOption by option(
        "--type",
        help = "Project type; only android is supported",
    )

    private val outputPath by option(
        "--output",
        "-o",
        help = "Output directory; defaults to the current directory",
    ).default(".")

    private val templateSourceOption by option(
        "--template",
        help = "Template Git URL (HTTPS or SSH)",
    ).default(Template.ANDROID_URL)

    private val authOption by option(
        "--auth",
        help = "Clone authentication: none, login, token",
    ).default("none")

    private val githubTokenOption by option(
        "--token",
        help = "GitHub token (required with --auth token; may use GITHUB_TOKEN env)",
    )

    private val githubUsernameOption by option(
        "--username",
        help = "GitHub username for token HTTPS clone (default: x-access-token)",
    ).default("x-access-token")

    override fun run() {
        try {
            val type = resolveProjectType()
            val projectName = resolveProjectName()
            val packageName = resolvePackageName()
            val authMode = resolveCloneAuthMode()
            val githubToken = resolveGitHubToken(authMode)

            validateProjectName(projectName)
            validatePackageName(packageName)

            val outputDir = File(outputPath).absoluteFile
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            require(outputDir.isDirectory) { "Output path is not a directory: ${outputDir.absolutePath}" }

            val targetDir = outputDir.resolve(projectName)
            ensureTargetDirectory(targetDir)

            echo(CliFormatter.formatInfo("Cloning template: $templateSourceOption (auth=${authMode.value})"))
            cloneTemplate(
                templateUrl = templateSourceOption,
                targetDir = targetDir,
                authMode = authMode,
                githubUsername = githubUsernameOption,
                githubToken = githubToken,
            )

            echo(CliFormatter.formatInfo("Replacing project name and package..."))
            customizeTemplate(targetDir, projectName, packageName)

            echo(CliFormatter.formatInfo("Initializing .egs config..."))
            val config = initializer.initialize(targetDir)

            echo()
            echo(CliFormatter.formatSuccess("Project created"))
            echo("  Path: ${targetDir.absolutePath}")
            echo("  Type: $type")
            echo("  Name: $projectName")
            echo("  Package: $packageName")
            echo("  .egs: ${targetDir.resolve(".egs/config.json").absolutePath}")
            echo("  Detected type: ${config.projectType}")
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Create project failed: ${e.message}"), err = true)
        }
    }

    private fun resolveProjectType(): String {
        val value = typeOption?.trim()?.takeIf { it.isNotBlank() } ?: "android"
        require(value.equals("android", ignoreCase = true)) {
            "Only android is supported; got: $value"
        }
        return "android"
    }

    private fun resolveProjectName(): String =
        projectNameArg?.trim()?.takeIf { it.isNotBlank() } ?: promptProjectName()

    private fun resolvePackageName(): String =
        packageNameOption?.trim()?.takeIf { it.isNotBlank() } ?: promptPackageName()

    private fun resolveCloneAuthMode(): CloneAuthMode =
        CloneAuthMode.from(authOption)
            ?: throw IllegalArgumentException("Unsupported --auth: $authOption (use: none, login, token)")

    private fun resolveGitHubToken(mode: CloneAuthMode): String? {
        if (mode != CloneAuthMode.TOKEN) {
            return null
        }

        val token = githubTokenOption?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GITHUB_TOKEN")?.trim()?.takeIf { it.isNotBlank() }

        require(!token.isNullOrBlank()) {
            "--auth token requires --token or GITHUB_TOKEN"
        }

        return token
    }

    private fun promptProjectName(): String {
        print("? Project name: ")
        val value = readLine()?.trim().orEmpty()
        require(value.isNotBlank()) { "Project name cannot be empty" }
        return value
    }

    private fun promptPackageName(): String {
        val defaultPackage = "com.dqc.example"
        print("? Package name [$defaultPackage]: ")
        val value = readLine()?.trim().orEmpty()
        return if (value.isBlank()) defaultPackage else value
    }

    private fun validateProjectName(projectName: String) {
        val nameRegex = Regex("^[A-Za-z][A-Za-z0-9_-]*$")
        require(nameRegex.matches(projectName)) {
            "Invalid project name: $projectName (must start with a letter; only letters, digits, - and _)"
        }
    }

    private fun validatePackageName(packageName: String) {
        val packageRegex = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
        require(packageRegex.matches(packageName)) {
            "Invalid package name: $packageName (example: com.dqc.workflow)"
        }
    }

    private fun ensureTargetDirectory(targetDir: File) {
        if (!targetDir.exists()) {
            return
        }
        require(targetDir.isDirectory) { "Target path exists and is not a directory: ${targetDir.absolutePath}" }
        require(targetDir.listFiles().isNullOrEmpty()) {
            "Target directory exists and is not empty: ${targetDir.absolutePath}"
        }
    }

    private fun cloneTemplate(
        templateUrl: String,
        targetDir: File,
        authMode: CloneAuthMode,
        githubUsername: String,
        githubToken: String?,
    ) {
        val parentDir = targetDir.parentFile ?: File(".")
        val result = when (authMode) {
            CloneAuthMode.NONE -> {
                exec(
                    command = listOf("git", "clone", "--depth", "1", templateUrl, targetDir.absolutePath),
                    workDir = parentDir,
                )
            }

            CloneAuthMode.LOGIN -> {
                prepareGitHubLoginIfNeeded(templateUrl, parentDir)
                exec(
                    command = listOf("git", "clone", "--depth", "1", templateUrl, targetDir.absolutePath),
                    workDir = parentDir,
                    environment = mapOf("GIT_TERMINAL_PROMPT" to "0"),
                )
            }

            CloneAuthMode.TOKEN -> {
                val env = mutableMapOf("GIT_TERMINAL_PROMPT" to "0")
                if (isGitHubHttpsUrl(templateUrl)) {
                    val token = githubToken ?: error("githubToken must not be null")
                    val raw = "$githubUsername:$token"
                    val encoded = Base64.getEncoder().encodeToString(raw.toByteArray())
                    env["GIT_HTTP_EXTRAHEADER"] = "Authorization: Basic $encoded"
                }

                exec(
                    command = listOf("git", "clone", "--depth", "1", templateUrl, targetDir.absolutePath),
                    workDir = parentDir,
                    environment = env,
                )
            }
        }

        if (result.exitCode != 0) {
            throw IllegalStateException(
                "Template clone failed: ${result.output.ifBlank { "git clone exitCode=${result.exitCode}" }}",
            )
        }

        targetDir.resolve(".git").takeIf { it.exists() }?.deleteRecursively()
    }

    private fun customizeTemplate(
        projectDir: File,
        projectName: String,
        packageName: String,
    ) {
        TemplatePackageRewriter().rewriteForward(
            projectDir = projectDir,
            recipe = TemplateRenameRecipes.ANDROID,
            newProjectName = projectName,
            newPackage = packageName,
        )
    }

    private fun prepareGitHubLoginIfNeeded(templateUrl: String, workDir: File) {
        if (!isGitHubHttpsUrl(templateUrl)) {
            return
        }

        val ghVersion = exec(listOf("gh", "--version"), workDir = workDir)
        require(ghVersion.exitCode == 0) {
            "--auth login requires GitHub CLI (gh); install it or use --auth token"
        }

        val status = exec(listOf("gh", "auth", "status"), workDir = workDir)
        if (status.exitCode != 0) {
            echo(CliFormatter.formatInfo("Not logged in to GitHub; starting gh auth login..."))
            val loginExitCode = execInteractive(
                command = listOf("gh", "auth", "login"),
                workDir = workDir,
            )
            require(loginExitCode == 0) {
                "GitHub login failed; retry or use --auth token"
            }
        }

        val setup = exec(listOf("gh", "auth", "setup-git"), workDir = workDir)
        require(setup.exitCode == 0) {
            "gh auth setup-git failed: ${setup.output.ifBlank { "exitCode=${setup.exitCode}" }}"
        }
    }

    private fun isGitHubHttpsUrl(url: String): Boolean =
        url.startsWith("https://github.com/", ignoreCase = true) ||
            url.startsWith("http://github.com/", ignoreCase = true)

    private fun exec(command: List<String>, workDir: File, environment: Map<String, String> = emptyMap()): ProcessResult {
        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)

        environment.forEach { (key, value) ->
            process.environment()[key] = value
        }

        val running = process.start()

        val output = running.inputStream.bufferedReader().readText().trim()
        val exitCode = running.waitFor()
        return ProcessResult(exitCode, output)
    }

    private fun execInteractive(command: List<String>, workDir: File): Int {
        val process = ProcessBuilder(command)
            .directory(workDir)
            .inheritIO()
            .start()
        return process.waitFor()
    }

    private data class ProcessResult(
        val exitCode: Int,
        val output: String,
    )

    private enum class CloneAuthMode(val value: String) {
        NONE("none"),
        LOGIN("login"),
        TOKEN("token"),
        ;

        companion object {
            fun from(value: String): CloneAuthMode? =
                entries.firstOrNull { it.value == value.lowercase() }
        }
    }

    private object Template {
        const val ANDROID_URL = "git@github.com:Dqc117266/egs-android-template.git"
    }
}
