[gd_scene load_steps=2 format=3 uid="uid://egs_${name}"]

[ext_resource type="Script" path="res://modules/${module}/src/${pascal}.gd" id="1_stub"]

[node name="${pascal}" type="Control"]
anchors_preset = 15
anchor_right = 1.0
anchor_bottom = 1.0
grow_horizontal = 2
grow_vertical = 2
script = ExtResource("1_stub")

[node name="CloseButton" type="Button" parent="."]
anchor_left = 1.0
anchor_top = 0.0
anchor_right = 1.0
anchor_bottom = 0.0
offset_left = -120.0
offset_top = 16.0
offset_right = -16.0
offset_bottom = 56.0
text = "Close"
