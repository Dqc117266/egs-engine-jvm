extends "${generatedExtendsPath}"
class_name ${pascal}Generated
## ${pascal}Generated — REGENERABLE (metroidvania theme).
## Tracks exploration state and minimap metadata.


func _ready() -> void:
	super._ready()
	room_id = "${name}"
	display_name = "${pascal}"
