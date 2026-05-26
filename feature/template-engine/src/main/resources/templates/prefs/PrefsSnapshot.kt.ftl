package ${packageName}.generate.data.datasource.preferences.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ${className}(
<#list fields as f>
    val ${f.propertyName}: ${f.kotlinType} = ${f.defaultExpr},
</#list>
)
