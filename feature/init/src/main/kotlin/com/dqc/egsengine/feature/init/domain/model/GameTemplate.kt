package com.dqc.egsengine.feature.init.domain.model

/**
 * Godot game template flavour, chosen at `egs new game --template <flavour>` time.
 *
 * The flavour is recorded in `.egs/workspace.json` (`projects.game.gameTemplate`) and
 * later read by `egs game add` to decide which field overlay each generated entity
 * gets. New flavours are registered here; the matching FTL branches live under
 * `feature/template-engine/.../templates/godot/entity/`.
 *
 * See `egs-godot-template/docs/GAME_TYPES.md` for the field matrix.
 */
enum class GameTemplate(val id: String) {
    BASE("base"),
    METROIDVANIA("metroidvania"),
    ;

    companion object {
        /** Parse a CLI/template id to a [GameTemplate], or null when unknown. */
        fun fromId(raw: String?): GameTemplate? = raw?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { value -> entries.firstOrNull { entry -> entry.id.equals(value, ignoreCase = true) } }
    }
}
