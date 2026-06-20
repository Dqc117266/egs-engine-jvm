extends "res://modules/combat/api/Enemy.gd"
class_name SlimeGenerated
## SlimeGenerated — REGENERABLE. Produced by `egs add enemy slime`.
##
## Do NOT hand-edit; rebuild with `egs add enemy slime --force`. Theme
## fields live here. User behaviour goes in src/.../Slime.gd (the stub).


func _ready() -> void:
	super._ready()
	display_name = "Slime"
