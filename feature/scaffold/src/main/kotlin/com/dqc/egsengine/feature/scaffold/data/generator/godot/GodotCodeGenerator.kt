package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import java.io.File

/**
 * Contract-driven Godot entity generator. Reads `.egs/generator.json`, resolves the
 * command spec, and renders each declared template via FreeMarker with theme overlay
 * (project `.egs/templates/godot/<theme>/` first, then `base/`, then bundled fallback).
 *
 * Pure preview — does NOT write disk. The scaffolder applies the planned files.
 * Mirrors the Python `tools/egs_godot_gen.py` `plan_entity` semantics.
 *
 * @see GodotFileMode for per-file create/overwrite rules
 */
class GodotCodeGenerator(
    private val templateEngine: TemplateEngine,
    private val workspaceReader: WorkspaceConfigReader = WorkspaceConfigReader(),
) {
    /**
     * Plan generation for [command] (e.g. `enemy`) named [rawName] under [projectRoot].
     *
     * @param themeOverride `--theme` value; null reads `workspace.json` `gameTemplate`, else the contract default.
     * @param force when true, existing non-user files are marked for rebuild instead of blocked.
     */
    fun preview(
        projectRoot: File,
        contract: GodotGeneratorContract,
        command: String,
        rawName: String,
        themeOverride: String?,
        force: Boolean,
    ): GodotCodePreview {
        val spec =
            contract.commands[command]
                ?: error("Unknown entity type '$command'. Valid: ${contract.commands.keys.sorted()}.")
        val theme = resolveTheme(projectRoot, contract, themeOverride)
        require(theme in contract.templates.themes || theme == contract.templates.defaultTheme) {
            "Unknown theme '$theme'. Valid: ${contract.templates.themes}."
        }

        val names = GodotPathInterpolator.namesFor(rawName, spec, command, contract.naming.inputPattern)
        val artifacts = GodotCommandSpecResolver.resolveArtifacts(spec, names)

        val ctx = buildContext(contract, spec, names, theme)
        val planned =
            artifacts.map { art ->
                val content = renderArtifact(projectRoot, command, theme, art.templateId, ctx)
                val target = projectRoot.resolve(art.relPath)
                val action = computeAction(target, art.mode, force)
                GodotPlannedFile(
                    relPath = art.relPath,
                    content = content,
                    mode = art.mode,
                    action = action,
                )
            }

        val registryScriptRes =
            spec.registryScriptPath
                ?.let { GodotPathInterpolator.interpolate(it, names) }

        return GodotCodePreview(
            command = command,
            module = names.module,
            theme = theme,
            pascal = names.pascal,
            snake = names.snake,
            files = planned,
            registers = spec.registers,
            registryScriptRes = registryScriptRes,
            registryRelativePath = contract.registry.relativePath,
        )
    }

    private fun resolveTheme(
        projectRoot: File,
        contract: GodotGeneratorContract,
        override: String?,
    ): String {
        if (!override.isNullOrBlank()) return override
        val wsFile = projectRoot.resolve(".egs/workspace.json")
        if (wsFile.exists()) {
            val game =
                runCatching { workspaceReader.read(projectRoot).projects["game"] }.getOrNull()
            val fromWs = game?.gameTemplate
            if (!fromWs.isNullOrBlank()) return fromWs
        }
        return contract.templates.defaultTheme
    }

    private fun buildContext(
        contract: GodotGeneratorContract,
        spec: GodotCommandSpec,
        names: GodotPathInterpolator.Names,
        theme: String,
    ): Map<String, Any> {
        val generatedExtends =
            spec.generatedExtendsPath
                ?.let { GodotPathInterpolator.interpolate(it, names) }
                .orEmpty()
        val userExtends =
            spec.userExtendsPath
                ?.let { GodotPathInterpolator.interpolate(it, names) }
                .orEmpty()
        return mapOf(
            "name" to names.rawName,
            "snake" to names.snake,
            "pascal" to names.pascal,
            "theme" to theme,
            "module" to names.module,
            "generatedExtendsPath" to generatedExtends,
            "userExtendsPath" to userExtends,
        )
    }

    private fun renderArtifact(
        projectRoot: File,
        command: String,
        theme: String,
        templateId: String,
        ctx: Map<String, Any>,
    ): String {
        // Theme overlay: theme dir first, then base fallback. Both the project's
        // .egs/templates/ and the bundled classpath carry this layout.
        val candidates =
            listOf(
                "godot/$theme/$command/$templateId",
                "godot/base/$command/$templateId",
            )
        return templateEngine.renderWithFallback(candidates, ctx, projectRoot)
    }

    private fun computeAction(
        target: File,
        mode: GodotFileMode,
        force: Boolean,
    ): GodotFileAction {
        if (!target.exists()) return GodotFileAction.CREATE
        // Existing file: user stubs are NEVER overwritten; rebuildables need --force.
        return when (mode) {
            GodotFileMode.USER_CREATE_ONCE -> GodotFileAction.SKIP_USER
            else -> if (force) GodotFileAction.REBUILD else GodotFileAction.BLOCKED
        }
    }
}

/** One planned output file with its create/overwrite decision. */
data class GodotPlannedFile(
    val relPath: String,
    val content: String,
    val mode: GodotFileMode,
    val action: GodotFileAction,
) {
    /** Adapt to the shared [GeneratedFile] used by the scaffolder's write phase. */
    fun toGeneratedFile(): GeneratedFile = GeneratedFile(relPath, content)
}

/** What the generator will do with a file relative to current disk state. */
enum class GodotFileAction {
    CREATE,
    SKIP_USER,
    REBUILD,
    BLOCKED,
}

/** Full preview of one `game add` invocation. */
data class GodotCodePreview(
    val command: String,
    val module: String,
    val theme: String,
    val pascal: String,
    val snake: String,
    val files: List<GodotPlannedFile>,
    val registers: Boolean,
    /** Raw `modules/...` path of the registered Generated script (fed to the registry lineFormat, which adds `res://`); null when [registers] is false. */
    val registryScriptRes: String?,
    /** Registry path relative to the module root, e.g. `generated/registry.gd`. */
    val registryRelativePath: String,
) {
    /** Relative path of the module's registry: `modules/<module>/<registryRelativePath>`. */
    val registryModuleRelPath: String get() = "modules/$module/$registryRelativePath"
}
