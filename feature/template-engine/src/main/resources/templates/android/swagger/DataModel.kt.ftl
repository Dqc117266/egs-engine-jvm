package ${packageName}

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
) {
    internal fun toDomain(): ${domainClassName} {
        return ${domainClassName}(
<#list props as p>
            ${p.name} = ${p.toDomainExpr},
</#list>
        )
    }
<#if hasToData>
    internal fun toData(): ${className} {
        return ${className}(
<#list props as p>
            ${p.name} = ${p.toDataExpr},
</#list>
        )
    }
</#if>
}
