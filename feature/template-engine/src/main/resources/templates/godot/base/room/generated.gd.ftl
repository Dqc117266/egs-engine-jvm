extends "${generatedExtendsPath}"
class_name ${pascal}Generated
## ${pascal}Generated — REGENERABLE. Produced by `egs add room ${name}`.
## Do NOT hand-edit; rebuild with --force.


func _ready() -> void:
	super._ready()
	room_id = "${name}"
	display_name = "${pascal}"
