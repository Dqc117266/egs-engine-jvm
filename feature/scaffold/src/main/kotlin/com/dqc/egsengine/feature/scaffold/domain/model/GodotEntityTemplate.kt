package com.dqc.egsengine.feature.scaffold.domain.model

import com.dqc.egsengine.feature.init.domain.model.GameTemplate

/**
 * The three entity kinds `egs game add` knows how to scaffold in v1.
 *
 * Each [kind] maps to a base-class contract (see `egs-godot-template/docs/GENERATOR.md`),
 * an output directory under `entities/<plural>/`, and the file extensions produced.
 */
enum class GodotEntityKind(
    val id: String,
    val pluralDir: String,
    val baseScriptResPath: String,
    val godotNodeType: String,
) {
    ENEMY(
        id = "enemy",
        pluralDir = "enemies",
        baseScriptResPath = "res://entities/enemies/core/enemy.gd",
        godotNodeType = "CharacterBody2D",
    ),
    SKILL(
        id = "skill",
        pluralDir = "skills",
        baseScriptResPath = "res://entities/skills/core/skill.gd",
        godotNodeType = "Resource",
    ),
    ROOM(
        id = "room",
        pluralDir = "rooms",
        baseScriptResPath = "res://entities/rooms/core/room.gd",
        godotNodeType = "Node2D",
    ),
    ;

    companion object {
        fun fromId(raw: String?): GodotEntityKind? = raw?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { value -> entries.firstOrNull { it.id.equals(value, ignoreCase = true) } }
    }
}

/**
 * View model passed to the Godot entity FTL templates.
 *
 * Naming is derived once from the raw entity name ([name]) so the generator,
 * registry updater, and templates all agree on `snake`/`className`.
 */
data class GodotEntityTemplateModel(
    /** Raw entity name as typed on the CLI, already validated. */
    val name: String,
    /** snake_case file stem, e.g. `boss_arena`. */
    val snakeName: String,
    /** PascalCase `class_name`, e.g. `BossArena`. */
    val className: String,
    val kind: GodotEntityKind,
    val gameTemplate: GameTemplate,
    /** `res://` path of the base script this entity `extends`. */
    val baseScriptResPath: String,
    /** `res://` path of the generated script, e.g. `res://entities/enemies/slime.gd`. */
    val generatedScriptResPath: String,
) {
    /** Human label used as the default `display_name` value (e.g. `Boss Arena`). */
    val displayLabel: String
        get() = className

    /** Stable lower-case id consumed by FTL `<#if templateId == "metroidvania">` branches. */
    val templateId: String
        get() = gameTemplate.id
}
