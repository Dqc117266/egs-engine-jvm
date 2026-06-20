package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotNameUtil.toPascalCase
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotNameUtil.toSnakeCase

/**
 * Interpolates `{module}`, `{snake}`, `{pascal}` placeholders in contract path/extends
 * strings — the Kotlin equivalent of the Python tool's
 * `template.format(module=..., snake=..., pascal=...)`.
 *
 * FreeMarker handles `${var}` inside `.ftl` bodies; this handles the contract's
 * brace placeholders in the path metadata (e.g.
 * `modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd`).
 */
internal object GodotPathInterpolator {
    private val PLACEHOLDER = Regex("""\{(module|snake|pascal)\}""")

    data class Names(
        val rawName: String,
        val snake: String,
        val pascal: String,
        val module: String,
    )

    /** Compute [Names] for [rawName] against [commandId] + [spec]; validates the name. */
    fun namesFor(
        rawName: String,
        spec: GodotCommandSpec,
        commandId: String,
        inputPattern: String = GodotNameUtil.DEFAULT_INPUT_PATTERN,
    ): Names {
        GodotNameUtil.requireValidEntityName(rawName, inputPattern)
        val snake = toSnakeCase(rawName)
        val pascal = toPascalCase(rawName)
        // Resolve the module field; it may itself contain placeholders (the `module`
        // command uses module="{snake}"). Command id is the fallback.
        val rawModule = spec.module ?: commandId
        val module = interpolate(rawModule, snake, pascal, commandId)
        return Names(rawName, snake, pascal, module)
    }

    /** Interpolate a contract path string with the resolved [names]. */
    fun interpolate(
        template: String,
        names: Names,
    ): String = interpolate(template, names.snake, names.pascal, names.module)

    /** Interpolate with explicit values (used when module differs, e.g. bootstrapping). */
    fun interpolate(
        template: String,
        snake: String,
        pascal: String,
        module: String,
    ): String = PLACEHOLDER.replace(template) { match ->
        when (match.groupValues[1]) {
            "module" -> module
            "snake" -> snake
            "pascal" -> pascal
            else -> match.value
        }
    }

    /** Convert a `modules/...` relative path to its `res://` form. */
    fun toResPath(relPath: String): String = "res://$relPath"
}
