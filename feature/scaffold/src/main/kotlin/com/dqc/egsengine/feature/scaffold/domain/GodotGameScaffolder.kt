package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
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
 * and rewrites the `game` entry of the cloned `.egs/workspace.json` (preserving its
 * `moduleStructure`/`baseClasses`) to record `gameTemplate` and the resolved template URL.
 *
 * The cloned `.egs/generator.json` and `.egs/templates/` are NEVER overwritten — they
 * are the authoritative contract the new `egs game add` commands read from.
 *
 * Post-clone, editor caches and build artifacts (`.godot/`, `.idea/`, `__pycache__/`,
 * `*.import`) are stripped as a safety net even though the upstream template ships clean.
 */
class GodotGameScaffolder(
    private val workspaceConfigWriter: WorkspaceConfigWriter,
    private val workspaceConfigReader: WorkspaceConfigReader = WorkspaceConfigReader(),
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
        val previewWorkspace = previewWorkspace(gameName, gameTemplate, resolvedUrl)

        if (dryRun) {
            return GodotGameResult(
                gameName = gameName,
                gameTemplate = gameTemplate,
                targetDir = targetDir,
                templateUrl = resolvedUrl,
                workspace = previewWorkspace,
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
        stripEditorArtifacts(targetDir, logInfo)
        mergeWorkspace(targetDir, gameName, gameTemplate, resolvedUrl)

        val finalWorkspace = workspaceConfigReader.read(targetDir)
        logger.info("Created Godot game '{}' ({}) at {}", gameName, gameTemplate.id, targetDir.absolutePath)

        return GodotGameResult(
            gameName = gameName,
            gameTemplate = gameTemplate,
            targetDir = targetDir,
            templateUrl = resolvedUrl,
            workspace = finalWorkspace,
            dryRun = false,
        )
    }

    /** Workspace shown in `--dry-run` (minimal; the real one is merged from the cloned template). */
    private fun previewWorkspace(
        gameName: String,
        gameTemplate: GameTemplate,
        resolvedUrl: String,
    ): WorkspaceConfig = WorkspaceConfig(
        name = gameName,
        version = "3",
        projects =
        mapOf(
            "game" to
                SubProjectConfig(
                    platform = Platform.GODOT,
                    path = ".",
                    basePackage = "",
                    engine = "godot",
                    gameTemplate = gameTemplate.id,
                    templateUrl = resolvedUrl,
                ),
        ),
    )

    /**
     * Rewrite the cloned workspace's `game` entry, preserving `moduleStructure`/`baseClasses`.
     * The cloned `.egs/generator.json` and `.egs/templates/` are left untouched.
     */
    private fun mergeWorkspace(
        targetDir: File,
        gameName: String,
        gameTemplate: GameTemplate,
        resolvedUrl: String,
    ) {
        val wsFile = targetDir.resolve(".egs/workspace.json")
        val merged =
            if (wsFile.exists()) {
                val cloned = workspaceConfigReader.read(targetDir)
                val game = cloned.projects["game"]
                val updatedGame =
                    (game ?: SubProjectConfig(Platform.GODOT, ".", "")).copy(
                        platform = Platform.GODOT,
                        engine = "godot",
                        gameTemplate = gameTemplate.id,
                        templateUrl = resolvedUrl,
                    )
                cloned.copy(
                    name = gameName,
                    version = "3",
                    projects = cloned.projects + ("game" to updatedGame),
                )
            } else {
                previewWorkspace(gameName, gameTemplate, resolvedUrl)
            }
        workspaceConfigWriter.write(merged, targetDir)
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

    /** Remove editor caches / build artifacts that should never ship in a fresh project. */
    private fun stripEditorArtifacts(projectRoot: File, logInfo: (String) -> Unit) {
        val targets = mutableListOf<File>()
        targets.add(projectRoot.resolve(".godot"))
        targets.add(projectRoot.resolve(".idea"))
        projectRoot.walkTopDown().onEnter { dir -> dir != projectRoot.resolve(".git") }.forEach { f ->
            when {
                f.isDirectory && f.name == "__pycache__" -> targets.add(f)
                f.isFile && (f.extension == "import" || f.name == ".DS_Store") -> targets.add(f)
            }
        }
        targets.distinct().forEach { f ->
            if (f.exists()) {
                f.deleteRecursively()
                logInfo("Stripped editor artifact: ${projectRoot.relativeTo(projectRoot).path}${f.relativeTo(projectRoot)}")
            }
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
