class_name ${pascal}
extends Control
## ${pascal} — user stub. Created once by `egs add ui ${name}`.
## Edit freely; never overwritten. Push/pop via UIManager.

signal closed


func _ready() -> void:
	pass


func _on_close_pressed() -> void:
	closed.emit()
