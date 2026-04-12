package ${packageName}

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
<#if imports?has_content>
<#list imports as imp>
import ${imp}
</#list>

</#if>
@Serializable
data class ${className}(
<#list props as p>
    @SerialName("${p.originalName}")
<#if p.nullable>
    val ${p.name}: ${p.kotlinType}? = null,
<#else>
    val ${p.name}: ${p.kotlinType},
</#if>
</#list>
)

internal fun ${className}.toDomain(): ${domainSimpleName} = ${domainSimpleName}(
<#list props as p>
    ${p.name} = ${p.toDomainExpr},
</#list>
)

<#if hasToData>
internal fun ${domainSimpleName}.toData(): ${className} = ${className}(
<#list props as p>
    ${p.name} = ${p.toDataExpr},
</#list>
)
</#if>
