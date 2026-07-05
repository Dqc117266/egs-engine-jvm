package com.dqc.egsengine.feature.scaffold.data.generator.godot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Deserialized form of a Godot project's `.egs/generator.json` — the single source of
 * truth for entity generation. Mirrors the contract consumed by the template's Python
 * tool (`tools/egs_godot_gen.py`).
 *
 * @see GodotGeneratorContractReader
 */
@Serializable
data class GodotGeneratorContract(
    val version: String = "2",
    val description: String? = null,
    val naming: GodotNamingContract = GodotNamingContract(),
    val registry: GodotRegistryContract = GodotRegistryContract(),
    val templates: GodotTemplateContract = GodotTemplateContract(),
    val commands: Map<String, GodotCommandSpec> = emptyMap(),
)

@Serializable
data class GodotNamingContract(
    val inputPattern: String = "^[A-Za-z][A-Za-z0-9_]*$",
    val fileNameCase: String = "snake_case",
    val classNameCase: String = "PascalCase",
)

@Serializable
data class GodotRegistryContract(
    /** `per-module` (only supported mode). */
    val mode: String = "per-module",
    /** Relative to a module root: `generated/registry.gd`. */
    val relativePath: String = "generated/registry.gd",
    /** Global aggregator, not written by the generator. */
    val globalAggregator: String = "app/autoload/EntityRegistry.gd",
    val beginMarker: String = "# === EGS-AUTOGEN-BEGIN ===",
    val endMarker: String = "# === EGS-AUTOGEN-END ===",
    /** `const {className} = preload("res://{scriptPath}")`. */
    val lineFormat: String = """const {className} = preload("res://{scriptPath}")""",
)

@Serializable
data class GodotTemplateContract(
    val root: String = ".egs/templates/godot",
    val defaultTheme: String = "base",
    val themes: List<String> = listOf("base", "metroidvania"),
)

/**
 * One entry under `commands` in generator.json. Path fields are templates with
 * `{module}`/`{snake}`/`{pascal}` placeholders; nullable when the command does
 * not produce that artifact kind (e.g. `item` has only `dataPath`).
 */
@Serializable
data class GodotCommandSpec(
    /** Module the entity lives in. May itself contain `{snake}` (the `module` command). */
    val module: String? = null,
    val summary: String? = null,
    val generatedScriptPath: String? = null,
    val userScriptPath: String? = null,
    val scenePath: String? = null,
    val dataPath: String? = null,
    val generatedExtendsPath: String? = null,
    val userExtendsPath: String? = null,
    @SerialName("moduleJsonPath") val moduleJsonPath: String? = null,
    @SerialName("readmePath") val readmePath: String? = null,
    val templateIds: List<String> = emptyList(),
    val registers: Boolean = false,
    val registryScriptPath: String? = null,
    /** If set, template lookup falls back to this entity's templates when the
     * command's own templates are absent (e.g. boss -> enemy, pickup -> enemy,
     * ability-lock -> room). Mirrors the Python tool's templateFallback. */
    val templateFallback: String? = null,
)
