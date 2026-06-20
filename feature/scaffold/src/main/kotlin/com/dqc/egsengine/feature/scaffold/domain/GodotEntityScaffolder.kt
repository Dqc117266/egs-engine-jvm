package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityPreview
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityRegistryUpdater
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Orchestrates `egs game add {enemy,skill,room} <name>` against a Godot project root.
 *
 * Flow:
 *  1. Resolve the `game` sub-project from `.egs/workspace.json` (must be [Platform.GODOT]).
 *  2. Preview files via [GodotEntityGenerator] (uses the workspace `gameTemplate` overlay).
 *  3. Guard: refuse to overwrite existing files (idempotent).
 *  4. Guard: refuse when EntityRegistry already declares the const.
 *  5. Write files and rewrite ONLY the EGS-AUTOGEN region of `autoload/EntityRegistry.gd`.
 *
 * With `dryRun = true` nothing is written; the returned [GodotScaffoldResult] carries
 * the would-be file list and the registry diff for display.
 */
class GodotEntityScaffolder(
    private val workspaceResolver: WorkspaceConfigResolver,
    private val generator: GodotEntityGenerator,
    private val registryUpdater: GodotEntityRegistryUpdater,
) {
    private val logger = LoggerFactory.getLogger(GodotEntityScaffolder::class.java)

    fun scaffold(
        gameProjectRoot: File,
        kind: GodotEntityKind,
        name: String,
        dryRun: Boolean = false,
    ): GodotScaffoldResult {
        val config = workspaceResolver.resolveByKey(gameProjectRoot, "game")
        require(config.platform == Platform.GODOT) {
            "Project 'game' is platform ${config.platform}, not GODOT. `egs game add` only targets Godot projects."
        }
        val gameTemplate = config.gameTemplateEnum()

        val preview = generator.preview(name, kind, gameTemplate)

        // Idempotency: never overwrite generated files.
        val existing = preview.files.filter { gameProjectRoot.resolve(it.path).exists() }
        require(existing.isEmpty()) {
            "Refusing to overwrite existing file(s): ${existing.joinToString { it.path }}. " +
                "Remove them first or choose a different name."
        }

        val registryFile = gameProjectRoot.resolve(REGISTRY_PATH)
        require(registryFile.exists()) {
            "EntityRegistry not found at ${registryFile.absolutePath}. " +
                "Ensure the project was created from the egs-godot-template."
        }
        val registryBefore = registryFile.readText()

        val registryUpdate =
            registryUpdater.addEntity(registryBefore, preview.model)

        if (!dryRun) {
            preview.files.forEach { file ->
                val target = gameProjectRoot.resolve(file.path)
                target.parentFile.mkdirs()
                target.writeText(file.content.orEmpty())
            }
            registryFile.writeText(registryUpdate.after)
            logger.info(
                "Generated {} '{}' ({} file(s)) and registered EntityRegistry.{}",
                kind.id,
                name,
                preview.files.size,
                preview.model.className,
            )
        }

        return GodotScaffoldResult(
            kind = kind,
            name = name,
            className = preview.model.className,
            gameTemplate = gameTemplate,
            files = preview.files,
            registryFile = REGISTRY_PATH,
            registryDiff = RegistryDiff(before = registryBefore, after = registryUpdate.after),
            dryRun = dryRun,
        )
    }

    /** Resolve the effective game template without scaffolding (used by `--dry-run` previews). */
    fun resolveGameTemplate(gameProjectRoot: File): GameTemplate = workspaceResolver.resolveByKey(gameProjectRoot, "game").gameTemplateEnum()

    companion object {
        const val REGISTRY_PATH = "autoload/EntityRegistry.gd"
    }
}

data class GodotScaffoldResult(
    val kind: GodotEntityKind,
    val name: String,
    val className: String,
    val gameTemplate: GameTemplate,
    val files: List<GeneratedFile>,
    val registryFile: String,
    val registryDiff: RegistryDiff,
    val dryRun: Boolean,
)

data class RegistryDiff(
    val before: String,
    val after: String,
)

/** Re-export for presentation-layer convenience. */
typealias GodotPreview = GodotEntityPreview
