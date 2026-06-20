[gd_resource type="Resource" script_class="${className}" load_steps=2 format=3]

[ext_resource type="Script" path="${generatedScriptResPath}" id="1_${snakeName}"]

[resource]
script = ExtResource("1_${snakeName}")
display_name = "${displayLabel}"
<#if templateId == "metroidvania">
mana_cost = 0.0
cooldown = 1.0
unlock_level = 1
</#if>
