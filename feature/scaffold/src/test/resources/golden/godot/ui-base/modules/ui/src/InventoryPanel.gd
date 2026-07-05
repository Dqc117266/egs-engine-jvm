class_name InventoryPanel
extends Control
## InventoryPanel — user stub. Created once by `egs add ui inventory_panel`.
## Edit freely; never overwritten. Push/pop via UIManager.

signal closed


func _ready() -> void:
	pass


func _on_close_pressed() -> void:
	closed.emit()
