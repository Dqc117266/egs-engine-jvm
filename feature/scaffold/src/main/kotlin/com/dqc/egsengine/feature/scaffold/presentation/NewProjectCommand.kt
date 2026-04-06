package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
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

    private val workspaceConfigWriter: WorkspaceConfigWriter by inject()

    private val projectNameArg by argument(help = "Project name").optional()

    private val packageNameOption by option(
        "--package",
        help = "Base package name, e.g. com.dqc.notes",
    )

    /** Omit to be prompted (defaults: kmp / true / true). */
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
        "--output", "-o",
        help = "Output directory (default: current dir)",
    ).default(".")

    private val githubTokenOption by option("--token", help = "GitHub PAT for HTTPS clone (private repos; password login is not supported)")
    private val githubUsernameOption by option("--username", help = "GitHub username for token URL (default: x-access-token)")
        .default("x-access-token")

    /** ssh uses ~/.ssh keys; https needs --token for private GitHub repos. Env: EGS_ENGINE_GIT_PROTOCOL */
    private val gitProtocolOption by option(
        "--protocol",
        help = "Clone URL scheme: ssh (default) or https",
    )

    private val clientTemplateOption by option("--client-template", help = "Override client template git URL")
    private val backendTemplateOption by option("--backend-template", help = "Override backend template git URL")
    private val webTemplateOption by option("--web-template", help = "Override web admin template git URL")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val projectName = resolveProjectName()
            val packageName = resolvePackageName()

            validateProjectName(projectName)
            validatePackageName(packageName)

            val clientRaw = resolveClientRaw()
            val includeBackend = resolveIncludeBackend()
            val includeWeb = resolveIncludeWeb()
            val includeClient = clientRaw != "none"
            val clientPlatform = resolveClientPlatform(clientRaw)
            val gitProtocol = resolveGitProtocol()

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
                val defaultUrl = defaultClientTemplateUrl(clientPlatform)
                val templateUrl = clientTemplateOption ?: defaultUrl
                    ?: throw IllegalArgumentException("No template URL for client platform $clientPlatform")
                val resolvedUrl = cloneUrlForProtocol(templateUrl, gitProtocol)
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would clone client ($clientPlatform) from $resolvedUrl -> $clientPath/"))
                } else {
                    val clientDir = targetDir.resolve(clientPath)
                    cloneAndCustomize(resolvedUrl, clientDir, projectName, packageName)
                }
                projects["client"] = SubProjectConfig(
                    platform = clientPlatform,
                    path = clientPath,
                    basePackage = packageName,
                    templateUrl = resolvedUrl,
                )
            }

            if (includeBackend) {
                val backendPath = "backend"
                val templateUrl = backendTemplateOption ?: TemplateUrls.BACKEND
                val resolvedUrl = cloneUrlForProtocol(templateUrl, gitProtocol)
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would clone backend from $resolvedUrl -> $backendPath/"))
                } else {
                    val backendDir = targetDir.resolve(backendPath)
                    cloneAndCustomize(resolvedUrl, backendDir, projectName, packageName)
                }
                projects["backend"] = SubProjectConfig(
                    platform = Platform.SPRING_BOOT,
                    path = backendPath,
                    basePackage = packageName,
                    templateUrl = resolvedUrl,
                )
            }

            if (includeWeb) {
                val adminPath = "admin"
                val templateUrl = webTemplateOption ?: TemplateUrls.ADMIN
                val resolvedUrl = cloneUrlForProtocol(templateUrl, gitProtocol)
                if (dryRun) {
                    echo(CliFormatter.formatInfo("[dry-run] Would clone admin from $resolvedUrl -> $adminPath/"))
                } else {
                    val adminDir = targetDir.resolve(adminPath)
                    cloneAndCustomize(resolvedUrl, adminDir, projectName, packageName)
                }
                projects["admin"] = SubProjectConfig(
                    platform = Platform.VUE3,
                    path = adminPath,
                    basePackage = packageName,
                    templateUrl = resolvedUrl,
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

    private fun resolveClientRaw(): String =
        clientOption?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: run {
                print("? Client Platform (kmp/android/none) [kmp]: ")
                readlnOrNull()?.trim()?.lowercase()?.ifBlank { "kmp" } ?: "kmp"
            }

    private fun resolveIncludeBackend(): Boolean =
        backendOption?.trim()?.takeIf { it.isNotBlank() }?.let { parseBoolFlag(it) }
            ?: promptYesNo("? Include Backend", defaultYes = true)

    private fun resolveIncludeWeb(): Boolean =
        webOption?.trim()?.takeIf { it.isNotBlank() }?.let { parseBoolFlag(it) }
            ?: promptYesNo("? Include Web Admin", defaultYes = true)

    private fun parseBoolFlag(s: String): Boolean =
        when (s.lowercase()) {
            "true", "yes", "y", "1" -> true
            "false", "no", "n", "0" -> false
            else -> throw IllegalArgumentException("Invalid boolean: $s (use true/false or y/n)")
        }

    private fun promptYesNo(message: String, defaultYes: Boolean): Boolean {
        val hint = if (defaultYes) "(Y/n)" else "(y/N)"
        print("$message $hint: ")
        val v = readlnOrNull()?.trim()?.lowercase()
        return when {
            v.isNullOrBlank() -> defaultYes
            v == "y" || v == "yes" -> true
            v == "n" || v == "no" -> false
            else -> defaultYes
        }
    }

    private fun resolveClientPlatform(clientRaw: String): Platform? =
        when (clientRaw) {
            "kmp" -> Platform.KMP
            "android" -> Platform.ANDROID
            "none" -> null
            else -> throw IllegalArgumentException("Unsupported client: $clientRaw (use: kmp, android, none)")
        }

    private fun defaultClientTemplateUrl(platform: Platform): String? =
        when (platform) {
            Platform.KMP, Platform.KMP_ANDROID -> TemplateUrls.KMP
            Platform.ANDROID -> TemplateUrls.ANDROID
            else -> null
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

    private fun resolveGitProtocol(): GitProtocol {
        val cli = gitProtocolOption?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (cli != null) {
            return when (cli) {
                "ssh" -> GitProtocol.SSH
                "https", "http" -> GitProtocol.HTTPS
                else -> throw IllegalArgumentException("Invalid --protocol: $cli (use ssh or https)")
            }
        }
        val env = System.getenv("EGS_ENGINE_GIT_PROTOCOL")?.trim()?.lowercase()
        if (env == "https" || env == "http") return GitProtocol.HTTPS
        if (env == "ssh") return GitProtocol.SSH
        return GitProtocol.SSH
    }

    private fun cloneUrlForProtocol(url: String, protocol: GitProtocol): String {
        when {
            url.startsWith("git@") || url.startsWith("ssh://") ->
                return when (protocol) {
                    GitProtocol.SSH -> url
                    GitProtocol.HTTPS -> githubSshToHttps(url) ?: url
                }
            url.startsWith("https://github.com/") ->
                return when (protocol) {
                    GitProtocol.HTTPS -> url
                    GitProtocol.SSH -> githubHttpsToSsh(url)
                }
        }
        return url
    }

    private fun githubHttpsToSsh(httpsUrl: String): String {
        var path = httpsUrl.removePrefix("https://github.com/").trimEnd('/')
        if (path.endsWith(".git")) path = path.dropLast(4)
        return "git@github.com:$path.git"
    }

    private fun githubSshToHttps(sshUrl: String): String? {
        val withoutGit = sshUrl.removeSuffix(".git")
        val path = when {
            sshUrl.startsWith("git@github.com:") ->
                withoutGit.removePrefix("git@github.com:")
            sshUrl.startsWith("ssh://git@github.com/") ->
                withoutGit.removePrefix("ssh://git@github.com/")
            else -> return null
        }
        return "https://github.com/${path.trim('/')}.git"
    }

    private fun effectiveCloneUrl(url: String): String {
        val token = githubTokenOption?.trim()?.takeIf { it.isNotBlank() } ?: return url
        if (!url.startsWith("https://github.com/")) return url
        val rest = url.removePrefix("https://github.com/")
        val user = githubUsernameOption.trim().ifBlank { "x-access-token" }
        return "https://$user:$token@github.com/$rest"
    }

    private fun cloneAndCustomize(canonicalUrl: String, targetDir: File, projectName: String, packageName: String) {
        val cloneUrl = effectiveCloneUrl(canonicalUrl)
        echo(CliFormatter.formatInfo("Cloning template: $canonicalUrl"))

        val parentDir = targetDir.parentFile ?: File(".")
        val result = exec(
            listOf("git", "clone", "--depth", "1", cloneUrl, targetDir.absolutePath),
            parentDir,
        )
        if (result.exitCode != 0) {
            throw IllegalStateException("Template clone failed: ${result.output.ifBlank { "exitCode=${result.exitCode}" }}")
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
        val replaced = ROOT_PROJECT_NAME_REGEX.replace(text) { mr ->
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

    private object TemplateUrls {
        const val KMP = "https://github.com/Dqc117266/egs-kmp-template.git"
        const val ANDROID = "https://github.com/Dqc117266/egs-android-template.git"
        const val BACKEND = "https://github.com/Dqc117266/egs-server-template.git"
        const val ADMIN = "https://github.com/Dqc117266/egs-admin-template.git"
    }

    private enum class GitProtocol {
        HTTPS,
        SSH,
    }

    companion object {
        private val ROOT_PROJECT_NAME_REGEX = Regex("""rootProject\.name\s*=\s*"[^"]*"""")
        /** `spring.application.name` in application.yml. */
        private val SPRING_APP_NAME_BLOCK_REGEX = Regex(
            """(spring:\s*\r?\n\s*application:\s*\r?\n\s*name:\s*)(\S+)""",
        )
        private val PACKAGE_JSON_NAME_REGEX = Regex(""""name"\s*:\s*"[^"]*"""")
    }
}
