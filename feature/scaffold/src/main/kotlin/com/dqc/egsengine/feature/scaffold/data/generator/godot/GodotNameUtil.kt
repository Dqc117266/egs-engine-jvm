package com.dqc.egsengine.feature.scaffold.data.generator.godot

/**
 * Kotlin-native identifier conversion for Godot entity generation.
 *
 * Mirrors the intent of `egs-godot-template/core/util/NameUtil.gd`, but lives in the engine
 * so generation never depends on the Godot runtime (the template's `String.matches` regex
 * is unreliable at runtime; this is the authoritative implementation).
 *
 * Contract (see `docs/GENERATOR.md`):
 * - valid entity name: `^[A-Za-z][A-Za-z0-9_]*$`
 * - file stem: snake_case
 * - `class_name`: PascalCase
 */
internal object GodotNameUtil {
    private val VALID_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*$")

    /** `^[A-Za-z][A-Za-z0-9_]*$` — the only names `egs game add` accepts. */
    fun isValidEntityName(name: String): Boolean = VALID_NAME.matches(name)

    /**
     * Normalize to a snake_case stem. Accepts snake_case, PascalCase, or camelCase input
     * (already validated to be `[A-Za-z0-9_]`).
     *  - `"boss_arena"` -> `"boss_arena"`
     *  - `"BossArena"`  -> `"boss_arena"`
     *  - `"Fire__Ball"` -> `"fire_ball"`
     *  - `"HPMax"`      -> `"hp_max"` (runs of capitals are treated as one word)
     */
    fun toSnakeCase(name: String): String {
        // Insert a boundary before an uppercase letter that follows a lowercase letter or digit,
        // e.g. "BossArena" -> "Boss_Arena", "hp2Go" -> "hp2_Go". Acronyms (HPM) stay glued.
        val withBoundaries = name.replace(BOUNDARY_REGEX) { match ->
            "${match.groupValues[1]}_${match.groupValues[2]}"
        }
        return withBoundaries
            .split("_")
            .filter { it.isNotEmpty() }
            .joinToString("_") { it.lowercase() }
            .trim('_')
    }

    /** snake/Pascal/raw -> PascalCase `class_name`. `"boss_arena"` -> `"BossArena"`. */
    fun toPascalCase(name: String): String = name
        .replace(BOUNDARY_REGEX) { mr -> "${mr.groupValues[1]}_${mr.groupValues[2]}" }
        .split("_")
        .filter { it.isNotEmpty() }
        .joinToString("") { part ->
            part.take(1).uppercase() + part.drop(1).lowercase()
        }

    /** Throw a usage error for an invalid entity name, listing the rule. */
    fun requireValidEntityName(name: String) {
        require(isValidEntityName(name)) {
            "Invalid entity name '$name'. Must match ^[A-Za-z][A-Za-z0-9_]*\$ (letters, digits, underscore; must start with a letter)."
        }
    }

    /**
     * Boundary between a lowercase letter or digit and an uppercase letter.
     * Captures both sides so the joiner can be inserted between them.
     */
    private val BOUNDARY_REGEX = Regex("([a-z0-9])([A-Z])")
}
