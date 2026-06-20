package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import com.dqc.egsengine.feature.scaffold.data.GitHubCloneUrlPolicy
import com.dqc.egsengine.feature.scaffold.data.NewProjectTemplateUrls
import com.dqc.egsengine.feature.scaffold.data.ProjectTemplateCloner
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Backs `egs new game <name> --engine godot --template <flavour>`:
 * clones the egs-godot-template, rewrites `config/name` in `project.godot`,
 * and writes a `.egs/workspace.json` recording `platform=GODOT`,
 * `engine=godot`, and the chosen [GameTemplate].
 *
 * Unlike the multi-project `new project` flow, a Godot game is a single
 * self-contained project rooted at `<output>/<name>/`.
 */
class GodotGameScaffolder(
    private val workspaceConfigWriter: WorkspaceConfigWriter,
) {
    private val logger = LoggerFactory.getLogger(GodotGameScaffolder::class.java)

    fun scaffold(
        outputDir: File,
        gameName: String,
        gameTemplate: GameTemplate,
        canonicalTemplateUrl: String = NewProjectTemplateUrls.GODOT,
        gitProtocolCli: String? = null,
        githubToken: String? = null,
        githubUsername: String = "x-access-token",
        templateUrlOverride: String? = null,
        dryRun: Boolean = false,
        logInfo: (String) -> Unit = {},
    ): GodotGameResult {
        val rawUrl = templateUrlOverride ?: canonicalTemplateUrl
        val resolvedUrl = GitHubCloneUrlPolicy.cloneUrlForProtocol(
            rawUrl,
            GitHubCloneUrlPolicy.resolveProtocol(gitProtocolCli),
        )

        val targetDir = outputDir.absoluteFile.resolve(gameName)

        val gameProject =
            SubProjectConfig(
                platform = Platform.GODOT,
                path = ".",
                basePackage = "",
                engine = "godot",
                gameTemplate = gameTemplate.id,
                templateUrl = resolvedUrl,
            )
        val workspace =
            WorkspaceConfig(
                name = gameName,
                version = "2",
                projects = mapOf("game" to gameProject),
            )

        if (dryRun) {
            return GodotGameResult(
                gameName = gameName,
                gameTemplate = gameTemplate,
                targetDir = targetDir,
                templateUrl = resolvedUrl,
                workspace = workspace,
                dryRun = true,
            )
        }

        if (!outputDir.exists()) outputDir.mkdirs()
        require(outputDir.isDirectory) { "Output path is not a directory: ${outputDir.absolutePath}" }
        if (targetDir.exists()) {
            error("Target directory already exists: ${targetDir.absolutePath}")
        }
        targetDir.mkdirs()

        val cloner = ProjectTemplateCloner(githubToken, githubUsername, logInfo = logInfo)
        cloner.cloneAndCustomize(
            canonicalUrl = resolvedUrl,
            targetDir = targetDir,
            projectName = gameName,
            packageName = null,
        )
        rewriteGodotProjectName(targetDir, gameName)

        workspaceConfigWriter.write(workspace, targetDir)

        logger.info("Created Godot game '{}' ({}) at {}", gameName, gameTemplate.id, targetDir.absolutePath)

        return GodotGameResult(
            gameName = gameName,
            gameTemplate = gameTemplate,
            targetDir = targetDir,
            templateUrl = resolvedUrl,
            workspace = workspace,
            dryRun = false,
        )
    }

    /** Patch `config/name="..."` in `project.godot`. Idempotent; no-op if file absent. */
    private fun rewriteGodotProjectName(projectRoot: File, gameName: String) {
        val projectFile = projectRoot.resolve("project.godot")
        if (!projectFile.exists()) {
            logger.warn("project.godot not found in cloned Godot template; skipping config/name rewrite")
            return
        }
        val text = projectFile.readText()
        val replaced = rewriteConfigName(text, gameName)
        if (text != replaced) {
            projectFile.writeText(replaced)
        }
    }

    companion object {
        private val CONFIG_NAME_REGEX = Regex("""config/name\s*=\s*"[^"]*"""")

        /**
         * Pure rewrite of the `config/name` line in a `project.godot` body.
         * Only the name value changes; every other line is returned verbatim.
         */
        fun rewriteConfigName(projectGodotBody: String, gameName: String): String = CONFIG_NAME_REGEX.replace(projectGodotBody) { "config/name=\"$gameName\"" }
    }
}

data class GodotGameResult(
    val gameName: String,
    val gameTemplate: GameTemplate,
    val targetDir: File,
    val templateUrl: String,
    val workspace: WorkspaceConfig,
    val dryRun: Boolean,
)
