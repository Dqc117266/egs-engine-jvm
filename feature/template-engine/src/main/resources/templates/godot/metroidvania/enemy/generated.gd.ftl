extends "${generatedExtendsPath}"
class_name ${pascal}Generated
## ${pascal}Generated — REGENERABLE (metroidvania theme).
## Carries combat stats used by Hitbox/Hurtbox routing and a drop table.
## User behaviour goes in the stub.


func _ready() -> void:
	super._ready()
	display_name = "${pascal}"
	max_hp = 20.0
	hp = max_hp
