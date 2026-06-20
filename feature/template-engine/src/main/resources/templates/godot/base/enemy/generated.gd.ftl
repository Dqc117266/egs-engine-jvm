extends "${generatedExtendsPath}"
class_name ${pascal}Generated
## ${pascal}Generated — REGENERABLE. Produced by `egs add enemy ${name}`.
##
## Do NOT hand-edit; rebuild with `egs add enemy ${name} --force`. Theme
## fields live here. User behaviour goes in src/.../${pascal}.gd (the stub).


func _ready() -> void:
	super._ready()
	display_name = "${pascal}"
