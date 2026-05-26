package ${packageName}.generate.data.datasource.preferences.model

internal data class ${featurePascal}PrefsDefaults(
<#list scalars as s>
    val ${s.kotlinPropertyName}: ${s.kotlinType} = ${s.defaultValueExpr},
</#list>
<#list snapshots as snap>
    val ${snap.defaultPropertyName}: ${snap.className} = ${snap.className}(),
</#list>
<#list listQueryKeys as k>
    val ${k.kotlinPropertyName}: ${k.kotlinType} = ${k.defaultValueExpr},
</#list>
)
