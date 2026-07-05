package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotCodePreview
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotFileAction
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotGeneratorContractReader
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotModuleRegistryUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotPlannedFile
import com.dqc.egsengine.feature.scaffold.data.generator.godot.RegistryDiff
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Orchestrates `egs game add <command> <name>` against a Godot project root.
 *
 * Flow (mirrors the Python tool's `cmd_add`):
 *  1. Require `.egs/generator.json` + a `game` sub-project that is [Platform.GODOT].
 *  2. Preview files via [GodotCodeGenerator] (theme overlay).
 *  3. Block when generated files exist without `--force`; skip user stubs that exist.
 *  4. Write `create`/`rebuild` files; create module dirs for the `module` command.
 *  5. When the command `registers`, update the module's `generated/registry.gd` (idempotent).
 *
 * The global `app/autoload/EntityRegistry.gd` aggregator is never written.
 */
class GodotCodeScaffolder(
    private val workspaceResolver: WorkspaceConfigResolver,
    private val contractReader: GodotGeneratorContractReader,
    private val generator: GodotCodeGenerator,
) {
    private val logger = LoggerFactory.getLogger(GodotCodeScaffolder::class.java)

    fun scaffold(
        projectRoot: File,
        command: String,
        name: String,
        themeOverride: String? = null,
        force: Boolean = false,
        dryRun: Boolean = false,
    ): GodotScaffoldResult {
        requireGodotGame(projectRoot)
        val (contract, spec) = contractReader.requireCommand(projectRoot, command)
        val preview = generator.preview(projectRoot, contract, command, name, themeOverride, force)

        // Refuse to overwrite existing non-user files unless --force.
        val blocked = preview.files.filter { it.action == GodotFileAction.BLOCKED }
        require(blocked.isEmpty()) {
            "Refusing to overwrite existing files (use --force to rebuild Generated files):\n" +
                blocked.joinToString("\n") { "  ${it.relPath}" }
        }

        // Registry update (idempotent; never an error on duplicate).
        val registryDiff: RegistryDiff? =
            if (preview.registers && preview.registryScriptRes != null) {
                val updater = GodotModuleRegistryUpdater(contract.registry)
                updater.addConst(
                    projectRoot = projectRoot,
                    module = preview.module,
                    className = preview.pascal,
                    scriptRes = preview.registryScriptRes,
                )
            } else {
                null
            }

        if (!dryRun) {
            preview.files
                .filter { it.action == GodotFileAction.CREATE || it.action == GodotFileAction.REBUILD }
                .forEach { file ->
                    val target = projectRoot.resolve(file.relPath)
                    target.parentFile.mkdirs()
                    target.writeText(file.content)
                }
            if (command == "module") scaffoldModuleDirs(projectRoot, preview.module)
            if (registryDiff != null && registryDiff.added != null) {
                val regFile = projectRoot.resolve(preview.registryModuleRelPath)
                regFile.parentFile.mkdirs()
                regFile.writeText(registryDiff.after)
            }
            logger.info(
                "Generated {} '{}' ({} file(s)) in module '{}'.",
                command,
                name,
                preview.files.count { it.action == GodotFileAction.CREATE || it.action == GodotFileAction.REBUILD },
                preview.module,
            )
        }

        return GodotScaffoldResult(
            preview = preview,
            registryDiff = registryDiff,
            dryRun = dryRun,
            force = force,
        )
    }

    private fun requireGodotGame(projectRoot: File): SubProjectConfig {
        val config = workspaceResolver.resolveByKey(projectRoot, "game")
        require(config.platform == Platform.GODOT) {
            "Project 'game' is platform ${config.platform}, not GODOT. `egs game add` only targets Godot projects."
        }
        return config
    }

    /** Create the standard module subdirs with .gitkeep pins (the `module` command). */
    private fun scaffoldModuleDirs(projectRoot: File, module: String) {
        for (sub in MODULE_DIRS) {
            val dir = projectRoot.resolve("modules/$module/$sub")
            dir.mkdirs()
            val keep = dir.resolve(".gitkeep")
            if (!keep.exists()) keep.writeText("")
        }
    }

    companion object {
        val MODULE_DIRS = listOf("api", "generated", "src", "scenes", "resources", "tests")
    }
}

/** Result of one `game add` invocation. */
data class GodotScaffoldResult(
    val preview: GodotCodePreview,
    /** Registry before/after; null when the command does not register. [RegistryDiff.added] is null when idempotent. */
    val registryDiff: RegistryDiff?,
    val dryRun: Boolean,
    val force: Boolean,
) {
    val files: List<GodotPlannedFile> get() = preview.files
}
