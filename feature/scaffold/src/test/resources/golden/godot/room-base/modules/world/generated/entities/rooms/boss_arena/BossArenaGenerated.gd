extends "res://modules/world/api/Room.gd"
class_name BossArenaGenerated
## BossArenaGenerated — REGENERABLE. Produced by `egs add room boss_arena`.
## Do NOT hand-edit; rebuild with --force.


func _ready() -> void:
	super._ready()
	room_id = "boss_arena"
	display_name = "BossArena"
