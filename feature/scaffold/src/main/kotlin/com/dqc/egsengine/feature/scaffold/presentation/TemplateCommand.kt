package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.scaffold.data.template.DevProjectConfigLoader
import com.dqc.egsengine.feature.scaffold.data.template.TemplatePromoter
import com.dqc.egsengine.feature.scaffold.data.template.TemplateRenameRecipes
import com.dqc.egsengine.feature.scaffold.data.template.TemplateSyncBack
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class TemplateCommand : EgsCliCommand(name = "template") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): TemplateCommand = TemplateCommand().subcommands(TemplateSyncBackCommand(), TemplatePromoteFtlCommand())
    }
}

class TemplateSyncBackCommand : EgsCliCommand(name = "sync-back") {
    private val fromOption by option("--from", help = "Generated project directory (default: demo-app subproject)")
        .default(".")

    private val toOption by option("--to", help = "Template repository directory (auto from workspace.json when omitted)")

    private val recipeOption by option(
        "--recipe",
        help = "Template recipe: android, kmp, server, admin (auto-detect from --to when omitted)",
    )

    private val fromProjectName by option("--from-project-name", help = "Project name in generated tree")
    private val toProjectName by option("--to-project-name", help = "Project name in template repo")
    private val fromPackage by option("--from-package", help = "Base package in generated tree")
    private val toPackage by option("--to-package", help = "Base package in template repo")

    private val pathsOption by option(
        "--paths",
        help = "Comma-separated top-level paths to sync, e.g. core-base,build-logic",
    )

    private val dryRun by option("--dry-run", help = "Preview without writing").flag()

    override fun runCommand() {
        val fromDir = File(fromOption).absoluteFile
        require(fromDir.isDirectory) { "--from is not a directory: ${fromDir.absolutePath}" }

        val context = DevProjectConfigLoader.loadSyncContext(fromDir)
        val toDir = resolveTargetDir(context)
        val recipe = resolveRecipe(toDir)
        val paths =
            pathsOption
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()

        val resolvedFromProject = fromProjectName ?: context.projectName
        val resolvedToProject = toProjectName ?: recipe.oldProjectName ?: toDir.name
        val resolvedFromPackage = fromPackage ?: context.basePackage
        val resolvedToPackage = toPackage ?: recipe.oldPackage

        echo(CliFormatter.formatInfo("Sync-back ${fromDir.name} -> ${toDir.name} (recipe=${recipe.id})"))
        if (dryRun) {
            echo(CliFormatter.formatInfo("Dry run mode"))
        }

        val result =
            TemplateSyncBack().sync(
                fromDir = fromDir,
                toDir = toDir,
                recipe = recipe,
                fromProjectName = resolvedFromProject,
                fromPackage = resolvedFromPackage,
                toProjectName = resolvedToProject,
                toPackage = resolvedToPackage,
                pathFilters = paths,
                dryRun = dryRun,
            )

        echo()
        echo("Copied files (${result.copiedFiles.size}):")
        result.copiedFiles.forEach { echo("  $it") }

        if (result.rewrittenFiles.isNotEmpty()) {
            echo()
            echo("Rewritten files (${result.rewrittenFiles.size}):")
            result.rewrittenFiles.forEach { echo("  $it") }
        }

        echo()
        if (dryRun) {
            echo(CliFormatter.formatSuccess("Dry run complete — no files written"))
        } else {
            echo(CliFormatter.formatSuccess("Sync-back complete"))
        }
    }

    private fun resolveTargetDir(context: com.dqc.egsengine.feature.scaffold.data.template.ProjectSyncContext): File {
        val explicit = toOption?.let { File(it).absoluteFile }
        if (explicit != null) {
            require(explicit.isDirectory) { "--to is not a directory: ${explicit.absolutePath}" }
            return explicit
        }
        val templateDir = context.templateDir
        require(templateDir != null) {
            "Cannot resolve template directory. Pass --to or configure templateUrl in workspace.json"
        }
        return templateDir
    }

    private fun resolveRecipe(toDir: File): com.dqc.egsengine.feature.scaffold.data.template.TemplateRenameRecipe {
        recipeOption?.trim()?.takeIf { it.isNotEmpty() }?.let { id ->
            return when (id.lowercase()) {
                "android" -> TemplateRenameRecipes.ANDROID
                "kmp" -> TemplateRenameRecipes.KMP
                "server" -> TemplateRenameRecipes.SERVER
                "admin" -> TemplateRenameRecipes.ADMIN
                else -> throw IllegalArgumentException("Unknown --recipe: $id")
            }
        }
        return TemplateRenameRecipes.detectFromPath(toDir.absolutePath)
            ?: throw IllegalArgumentException(
                "Cannot detect template recipe from ${toDir.absolutePath}. Pass --recipe explicitly.",
            )
    }
}

class TemplatePromoteFtlCommand : EgsCliCommand(name = "promote-ftl") {
    private val fromOption by option(
        "--from",
        help = "Project .egs/templates directory (default: <project>/.egs/templates)",
    )

    private val toOption by option(
        "--to",
        help = "Bundled templates root (default: EGS_TEMPLATE_ROOT or egs-engine feature/template-engine/resources/templates)",
    )

    private val projectOption by option("--project", "-p", help = "Project root containing .egs/templates")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing").flag()

    override fun runCommand() {
        val projectRoot = File(projectOption).absoluteFile
        val sourceRoot =
            fromOption?.let { File(it).absoluteFile }
                ?: projectRoot.resolve(".egs/templates")
        require(sourceRoot.isDirectory) {
            "Template override directory not found: ${sourceRoot.absolutePath}"
        }

        val targetRoot = resolveTargetRoot(toOption)
        require(targetRoot.isDirectory) {
            "Target template root not found: ${targetRoot.absolutePath}. Set EGS_TEMPLATE_ROOT or pass --to."
        }

        echo(CliFormatter.formatInfo("Promote FTL ${sourceRoot.absolutePath} -> ${targetRoot.absolutePath}"))
        if (dryRun) {
            echo(CliFormatter.formatInfo("Dry run mode"))
        }

        val result =
            TemplatePromoter().promote(
                sourceRoot = sourceRoot,
                targetRoot = targetRoot,
                dryRun = dryRun,
            )

        echo()
        echo("Promoted templates (${result.promotedFiles.size}):")
        result.promotedFiles.forEach { echo("  $it") }

        echo()
        if (dryRun) {
            echo(CliFormatter.formatSuccess("Dry run complete — no files written"))
        } else {
            echo(CliFormatter.formatSuccess("Promote complete"))
        }
    }

    private fun resolveTargetRoot(explicit: String?): File {
        explicit?.trim()?.takeIf { it.isNotEmpty() }?.let { return File(it).absoluteFile }
        TemplatePromoter.resolveTargetRoot(null)?.let { return it.absoluteFile }

        val cwd = File(System.getProperty("user.dir")).absoluteFile
        val candidates =
            listOf(
                cwd.resolve("feature/template-engine/src/main/resources/templates"),
                cwd.resolve("egs-engine/feature/template-engine/src/main/resources/templates"),
                cwd.parentFile?.resolve("egs-engine/feature/template-engine/src/main/resources/templates"),
            ).filterNotNull()

        return candidates.firstOrNull { it.isDirectory }
            ?: throw IllegalArgumentException(
                "Cannot locate bundled templates. Set EGS_TEMPLATE_ROOT or pass --to.",
            )
    }
}
