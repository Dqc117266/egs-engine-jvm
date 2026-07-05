# Fishing

Fishing gameplay module.

## Layout

- `api/` — public base classes other modules may depend on.
- `generated/` — regenerable entity skeletons + `registry.gd` (managed by `egs add`).
- `src/` — your hand-written code. Never overwritten by the generator.
- `scenes/`, `resources/` — Godot scenes and `.tres` data.
- `tests/` — GUT tests for this module.

## Add entities

```
egs add enemy slime      # generated + stub + scene, registered here
egs add skill fireball
egs add room boss_arena
egs add item health_potion
egs add ui inventory_panel
```
