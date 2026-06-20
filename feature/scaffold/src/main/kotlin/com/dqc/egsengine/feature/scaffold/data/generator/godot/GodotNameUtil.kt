package com.dqc.egsengine.feature.scaffold.data.generator.godot

/**
 * Kotlin-native identifier conversion for Godot entity generation.
 *
 * Lives in the engine so generation never depends on the Godot runtime. The
 * authoritative valid-name rule comes from `.egs/generator.json`
 * (`naming.inputPattern`, default `^[A-Za-z][A-Za-z0-9_]*$`).
 */
internal object GodotNameUtil {
    const val DEFAULT_INPUT_PATTERN = "^[A-Za-z][A-Za-z0-9_]*\$"

    /** True when [name] matches [pattern] (default the contract pattern). */
    fun isValidEntityName(
        name: String,
        pattern: String = DEFAULT_INPUT_PATTERN,
    ): Boolean = Regex(pattern).matches(name)

    /**
     * Normalize to a snake_case stem. Accepts snake_case, PascalCase, or camelCase input.
     * For contract-supplied (already snake_case) names this is a no-op split/lower.
     *  - `"boss_arena"` -> `"boss_arena"`
     *  - `"BossArena"`  -> `"boss_arena"`
     */
    fun toSnakeCase(name: String): String {
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
    fun requireValidEntityName(
        name: String,
        pattern: String = DEFAULT_INPUT_PATTERN,
    ) {
        require(isValidEntityName(name, pattern)) {
            "Invalid entity name '$name'. Must match $pattern (letters, digits, underscore; must start with a letter)."
        }
    }

    /**
     * Boundary between a lowercase letter or digit and an uppercase letter.
     * Captures both sides so the joiner can be inserted between them.
     */
    private val BOUNDARY_REGEX = Regex("([a-z0-9])([A-Z])")
}
