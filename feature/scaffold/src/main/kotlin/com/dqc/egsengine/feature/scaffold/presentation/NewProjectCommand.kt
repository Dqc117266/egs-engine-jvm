package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.SwaggerSyncConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import com.dqc.egsengine.feature.scaffold.data.GitHubCloneUrlPolicy
import com.dqc.egsengine.feature.scaffold.data.NewProjectTemplateUrls
import com.dqc.egsengine.feature.scaffold.data.ProjectTemplateCloner
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject
import java.io.File

class NewCommand : EgsCliCommand(name = "new", help = "Create a new multi-platform workspace") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): NewCommand = NewCommand().subcommands(NewProjectCommand())
    }
}

class NewProjectCommand : EgsCliCommand(name = "project", help = "Create a new project workspace (client + backend + admin)") {
    private val workspaceConfigWriter: WorkspaceConfigWriter by inject()
    private val workspaceConfigResolver: WorkspaceConfigResolver by inject()

    private val projectNameArg by argument(help = "Project name").optional()

    private val packageNameOption by option(
        "--package",
        help = "Base package name, e.g. com.dqc.notes",
    )

    private val clientOption by option(
        "--client",
        help = "Client type: kmp, android, none",
    )

    private val backendOption by option(
        "--backend",
        help = "Include backend: true or false",
    )

    private val webOption by option(
        "--web",
        help = "Include web admin: true or false",
    )

    private val outputPath by option(
        "--output",
        "-o",
        help = "Output directory (default: current dir)",
    ).default(".")

    private val githubTokenOption by option(
        "--token",
        help = "GitHub PAT for HTTPS clone (private repos; password login is not supported)",
    )
    private val githubUsernameOption by option(
        "--username",
        help = "GitHub username for token URL (default: x-access-token)",
    ).default("x-access-token")

    private val gitProtocolOption by option(
        "--protocol",
        help = "Clone URL scheme: ssh (default) or https. Env: EGS_ENGINE_GIT_PROTOCOL",
    )

    private val clientTemplateOption by option("--client-template", help = "Override client template git URL")
    private val backendTemplateOption by option("--backend-template", help = "Override backend template git URL")
    private val webTemplateOption by option("--web-template", help = "Override web admin template git URL")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    private val nonInteractive by option(
        "--yes",
        "-y",
        help = "Non-interactive mode: use defaults for all prompts (CI-friendly)",
    ).flag()

    override fun runCommand() {
        val projectName = NewProjectInputResolver.resolveProjectName(projectNameArg, nonInteractive)
        val packageName = NewProjectInputResolver.resolvePackageName(packageNameOption, nonInteractive)

        NewProjectInputResolver.validateProjectName(projectName)
        NewProjectInputResolver.validatePackageName(packageName)

        val clientRaw = NewProjectInputResolver.resolveClientRaw(clientOption, nonInteractive)
        val includeBackend = NewProjectInputResolver.resolveIncludeBackend(backendOption, nonInteractive)
        val includeWeb = NewProjectInputResolver.resolveIncludeWeb(webOption, nonInteractive)
        val includeClient = clientRaw != "none"
        val clientPlatform = NewProjectInputResolver.resolveClientPlatform(clientRaw)
        val gitProtocol = GitHubCloneUrlPolicy.resolveProtocol(gitProtocolOption)

        val outputDir = File(outputPath).absoluteFile
        if (!outputDir.exists()) outputDir.mkdirs()
        require(outputDir.isDirectory) { "Output path is not a directory: ${outputDir.absolutePath}" }

        val targetDir = outputDir.resolve(projectName)
        if (!dryRun) {
            NewProjectInputResolver.ensureTargetDirectory(targetDir)
            targetDir.mkdirs()
        }

        val projects = mutableMapOf<String, SubProjectConfig>()

        val cloner =
            ProjectTemplateCloner(
                githubTokenOption,
                githubUsernameOption,
            ) { echo(CliFormatter.formatInfo(it)) }

        if (includeClient && clientPlatform != null) {
            val clientPath = "client"
            val defaultUrl = NewProjectInputResolver.defaultClientTemplateUrl(clientPlatform)
            val templateUrl =
                clientTemplateOption ?: defaultUrl
                    ?: throw IllegalArgumentException("No template URL for client platform $clientPlatform")
            val resolvedUrl = GitHubCloneUrlPolicy.cloneUrlForProtocol(templateUrl, gitProtocol)
            if (dryRun) {
                echo(CliFormatter.formatInfo("[dry-run] Would clone client ($clientPlatform) from $resolvedUrl -> $clientPath/"))
            } else {
                cloner.cloneAndCustomize(
                    canonicalUrl = resolvedUrl,
                    targetDir = targetDir.resolve(clientPath),
                    projectName = projectName,
                    packageName = packageName,
                )
            }
            projects["client"] =
                SubProjectConfig(
                    platform = clientPlatform,
                    path = clientPath,
                    basePackage = packageName,
                    templateUrl = resolvedUrl,
                )
        }

        if (includeBackend) {
            val backendPath = "backend"
            val templateUrl = backendTemplateOption ?: NewProjectTemplateUrls.BACKEND
            val resolvedUrl = GitHubCloneUrlPolicy.cloneUrlForProtocol(templateUrl, gitProtocol)
            if (dryRun) {
                echo(CliFormatter.formatInfo("[dry-run] Would clone backend from $resolvedUrl -> $backendPath/"))
            } else {
                cloner.cloneAndCustomize(resolvedUrl, targetDir.resolve(backendPath), projectName, packageName)
            }
            val backendConfig =
                if (dryRun) {
                    SubProjectConfig(
                        platform = Platform.SPRING_BOOT,
                        path = backendPath,
                        basePackage = packageName,
                        templateUrl = resolvedUrl,
                    )
                } else {
                    val backendRoot = targetDir.resolve(backendPath)
                    SubProjectConfig(
                        platform = Platform.SPRING_BOOT,
                        path = backendPath,
                        basePackage = workspaceConfigResolver.detectSpringBootBasePackage(backendRoot) ?: packageName,
                        templateUrl = resolvedUrl,
                        conventionPluginId = workspaceConfigResolver.detectSpringBootConventionPluginId(backendRoot),
                    )
                }
            projects["backend"] = backendConfig
        }

        if (includeWeb) {
            val adminPath = "admin"
            val templateUrl = webTemplateOption ?: NewProjectTemplateUrls.ADMIN
            val resolvedUrl = GitHubCloneUrlPolicy.cloneUrlForProtocol(templateUrl, gitProtocol)
            if (dryRun) {
                echo(CliFormatter.formatInfo("[dry-run] Would clone admin from $resolvedUrl -> $adminPath/"))
            } else {
                cloner.cloneAndCustomize(resolvedUrl, targetDir.resolve(adminPath), projectName)
            }
            projects["admin"] =
                SubProjectConfig(
                    platform = Platform.VUE3,
                    path = adminPath,
                    basePackage = packageName,
                    templateUrl = resolvedUrl,
                )
        }

        val swaggerConfig =
            if (includeBackend && (includeClient || includeWeb)) {
                SwaggerSyncConfig()
            } else {
                null
            }

        val workspaceConfig =
            WorkspaceConfig(
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
    }
}
