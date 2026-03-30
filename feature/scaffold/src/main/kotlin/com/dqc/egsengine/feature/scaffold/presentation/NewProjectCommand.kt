package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
import com.dqc.egsengine.feature.init.domain.ProjectInitializer
import com.dqc.egsengine.feature.init.domain.model.ModuleStructure
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.SwaggerSyncConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class NewCommand : CliktCommand(name = "new") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): NewCommand =
            NewCommand().subcommands(NewProjectCommand())
    }
}

class NewProjectCommand : CliktCommand(name = "project"), KoinComponent {

    private val initializer: ProjectInitializer by inject()
    private val workspaceConfigWriter: WorkspaceConfigWriter by inject()

    private val projectNameArg by argument(help = "Project name").optional()

    private val packageNameOption by option(
        "--package",
        help = "Base package name, e.g. com.dqc.notes",
    )

    private val clientOption by option(
        "--client",
        help = "Client type: kmp (default), android, none",
    ).default("kmp")

    private val backendOption by option(
        "--backend",
        help = "Include backend (true/false)",
    ).default("true")

    private val webOption by option(
        "--web",
        help = "Include web admin (true/false)",
    ).default("true")

    private val outputPath by option(
        "--output", "-o",
        help = "Output directory (default: current dir)",
    ).default(".")

    private val authOption by option(
        "--auth",
        help = "Clone auth mode: none, login, token",
    ).default("none")

    private val githubTokenOption by option("--token", help = "GitHub Token")
    private val githubUsernameOption by option("--username").default("x-access-token")

    private val clientTemplateOption by option("--client-template", help = "Client template git URL")
    private val backendTemplateOption by option("--backend-template", help = "Backend template git URL")
    private val webTemplateOption by option("--web-template", help = "Web admin template git URL")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val projectName = resolveProjectName()
            val packageName = resolvePackageName()
            val includeClient = clientOption.lowercase() != "none"
            val includeBackend = backendOption.toBoolean()
            val includeWeb = webOption.toBoolean()
            val clientPlatform = resolveClientPlatform()

            validateProjectName(projectName)
            validatePackageName(packageName)

            val outputDir = File(outputPath).absoluteFile
            if (!outputDir.exists()) outputDir.mkdirs()
            require(outputDir.isDirectory) { "Output path is not a directory: ${outputDir.absolutePath}" }

            val targetDir = outputDir.resolve(projectName)
            if (!dryRun) {
                ensureTargetDirectory(targetDir)
                targetDir.mkdirs()
            }

            val projects = mutableMapOf<String, SubProjectConfig>()

            if (includeClient && clientPlatform != null) {
                val clientPath = "client"
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would create client ($clientPlatform) at $clientPath/"))
                } else {
                    val clientDir = targetDir.resolve(clientPath)
                    val templateUrl = clientTemplateOption ?: TemplateUrls.CLIENT
                    cloneAndCustomize(templateUrl, clientDir, projectName, packageName)
                }
                projects["client"] = SubProjectConfig(
                    platform = clientPlatform,
                    path = clientPath,
                    basePackage = packageName,
                )
            }

            if (includeBackend) {
                val backendPath = "backend"
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would create backend (SPRING_BOOT) at $backendPath/"))
                } else {
                    val backendDir = targetDir.resolve(backendPath)
                    val templateUrl = backendTemplateOption
                    if (templateUrl != null) {
                        cloneAndCustomize(templateUrl, backendDir, projectName, packageName)
                    } else {
                        echo(CliFormatter.formatInfo("Backend template not yet configured -- creating empty directory"))
                        backendDir.mkdirs()
                    }
                }
                projects["backend"] = SubProjectConfig(
                    platform = Platform.SPRING_BOOT,
                    path = backendPath,
                    basePackage = packageName,
                )
            }

            if (includeWeb) {
                val adminPath = "admin"
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would create web admin (VUE3) at $adminPath/"))
                } else {
                    val adminDir = targetDir.resolve(adminPath)
                    val templateUrl = webTemplateOption
                    if (templateUrl != null) {
                        cloneAndCustomize(templateUrl, adminDir, projectName, packageName)
                    } else {
                        echo(CliFormatter.formatInfo("Web admin template not yet configured -- creating empty directory"))
                        adminDir.mkdirs()
                    }
                }
                projects["admin"] = SubProjectConfig(
                    platform = Platform.VUE3,
                    path = adminPath,
                    basePackage = packageName,
                )
            }

            val swaggerConfig = if (includeBackend && (includeClient || includeWeb)) {
                SwaggerSyncConfig()
            } else {
                null
            }

            val workspaceConfig = WorkspaceConfig(
                name = projectName,
                projects = projects,
                swagger = swaggerConfig,
            )

            if (!dryRun) {
                workspaceConfigWriter.write(workspaceConfig, targetDir)
            }

            echo()
            if (dryRun) {
                echo(CliFormatter.formatInfo("[dry-run] Would create workspace at ${targetDir.absolutePath}"))
            } else {
                echo(CliFormatter.formatSuccess("Workspace created"))
            }
            echo("  Name: $projectName")
            echo("  Package: $packageName")
            echo("  Projects: ${projects.keys.joinToString(", ")}")
            if (!dryRun) {
                echo("  Path: ${targetDir.absolutePath}")
                echo("  Config: ${targetDir.resolve(".egs/workspace.json").absolutePath}")
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to create project: ${e.message}"), err = true)
        }
    }

    private fun resolveClientPlatform(): Platform? = when (clientOption.lowercase()) {
        "kmp" -> Platform.KMP_ANDROID
        "android" -> Platform.ANDROID
        "none" -> null
        else -> throw IllegalArgumentException("Unsupported --client value: $clientOption (use: kmp, android, none)")
    }

    private fun resolveProjectName(): String =
        projectNameArg?.trim()?.takeIf { it.isNotBlank() }
            ?: run {
                print("? Project Name: ")
                val value = readlnOrNull()?.trim().orEmpty()
                require(value.isNotBlank()) { "Project name cannot be empty" }
                value
            }

    private fun resolvePackageName(): String =
        packageNameOption?.trim()?.takeIf { it.isNotBlank() }
            ?: run {
                val defaultPkg = "com.dqc.example"
                print("? Package Name [$defaultPkg]: ")
                val value = readlnOrNull()?.trim().orEmpty()
                value.ifBlank { defaultPkg }
            }

    private fun validateProjectName(name: String) {
        require(Regex("^[A-Za-z][A-Za-z0-9_-]*$").matches(name)) {
            "Invalid project name: $name (must start with letter, only [A-Za-z0-9_-])"
        }
    }

    private fun validatePackageName(name: String) {
        require(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$").matches(name)) {
            "Invalid package name: $name (example: com.dqc.notes)"
        }
    }

    private fun ensureTargetDirectory(targetDir: File) {
        if (!targetDir.exists()) return
        require(targetDir.isDirectory) { "Target path exists and is not a directory: ${targetDir.absolutePath}" }
        require(targetDir.listFiles().isNullOrEmpty()) { "Target directory is not empty: ${targetDir.absolutePath}" }
    }

    private fun cloneAndCustomize(templateUrl: String, targetDir: File, projectName: String, packageName: String) {
        echo(CliFormatter.formatInfo("Cloning template: $templateUrl"))

        val parentDir = targetDir.parentFile ?: File(".")
        val result = exec(
            listOf("git", "clone", "--depth", "1", templateUrl, targetDir.absolutePath),
            parentDir,
        )
        if (result.exitCode != 0) {
            throw IllegalStateException("Template clone failed: ${result.output.ifBlank { "exitCode=${result.exitCode}" }}")
        }

        targetDir.resolve(".git").takeIf { it.exists() }?.deleteRecursively()
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

    private object TemplateUrls {
        const val CLIENT = "git@github.com:Dqc117266/egs-android-template.git"
    }
}
