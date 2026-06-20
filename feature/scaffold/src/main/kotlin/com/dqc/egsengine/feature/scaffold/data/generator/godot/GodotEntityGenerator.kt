package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityTemplateModel
import com.dqc.egsengine.feature.templateengine.TemplateEngine

/**
 * Renders a single Godot entity (enemy / skill / room) into its file set via the
 * FreeMarker templates under `templates/godot/entity/`.
 *
 * Pure preview — does NOT touch disk. The scaffolder decides what to write and
 * whether to update the registry. Output paths mirror the template contract:
 *  - enemy: `entities/enemies/<snake>.gd` + `.tscn`
 *  - skill: `entities/skills/<snake>.gd`  + `.tres`
 *  - room:  `entities/rooms/<snake>.gd`   + `.tscn`
 *
 * Cache files (the `.godot/` folder, `.import` and `.uid` sidecars) are never produced.
 */
class GodotEntityGenerator(
    private val templateEngine: TemplateEngine,
) {
    /**
     * Build the model + rendered files for [rawName] of [kind] under [gameTemplate].
     */
    fun preview(
        rawName: String,
        kind: GodotEntityKind,
        gameTemplate: GameTemplate,
    ): GodotEntityPreview {
        GodotNameUtil.requireValidEntityName(rawName)
        val snake = GodotNameUtil.toSnakeCase(rawName)
        val className = GodotNameUtil.toPascalCase(rawName)

        val scriptDir = "entities/${kind.pluralDir}"
        val scriptRel = "$scriptDir/$snake.gd"
        val scriptRes = "res://$scriptRel"
        val model =
            GodotEntityTemplateModel(
                name = rawName,
                snakeName = snake,
                className = className,
                kind = kind,
                gameTemplate = gameTemplate,
                baseScriptResPath = kind.baseScriptResPath,
                generatedScriptResPath = scriptRes,
            )

        val files = mutableListOf<GeneratedFile>()
        files += GeneratedFile(scriptRel, templateEngine.render("godot/entity/${kind.id}.gd.ftl", model))

        when (kind) {
            GodotEntityKind.ENEMY, GodotEntityKind.ROOM -> {
                files += GeneratedFile("$scriptDir/$snake.tscn", templateEngine.render("godot/entity/${kind.id}.tscn.ftl", model))
            }
            GodotEntityKind.SKILL -> {
                files += GeneratedFile("$scriptDir/$snake.tres", templateEngine.render("godot/entity/${kind.id}.tres.ftl", model))
            }
        }

        return GodotEntityPreview(model = model, files = files)
    }
}

/** Snapshot of one entity generation: the resolved model + its generated files (relative paths). */
data class GodotEntityPreview(
    val model: GodotEntityTemplateModel,
    val files: List<GeneratedFile>,
) {
    /** Relative path of the generated script (e.g. `entities/enemies/slime.gd`). */
    val scriptRelPath: String get() = model.run { "entities/${kind.pluralDir}/$snakeName.gd" }

    /** `res://` path used by the registry preload() (e.g. `res://entities/enemies/slime.gd`). */
    val scriptResPath: String get() = model.generatedScriptResPath
}
