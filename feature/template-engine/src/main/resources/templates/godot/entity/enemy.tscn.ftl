[gd_scene load_steps=2 format=3]

[ext_resource type="Script" path="${generatedScriptResPath}" id="1_${snakeName}"]

[node name="${className}" type="${kind.godotNodeType}"]
script = ExtResource("1_${snakeName}")
display_name = "${displayLabel}"
max_hp = 20.0
