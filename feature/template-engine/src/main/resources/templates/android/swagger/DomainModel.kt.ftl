package ${packageName}

data class ${className}(
<#list props as p>
<#if p.nullable>
    val ${p.name}: ${p.kotlinType}? = null,
<#else>
    val ${p.name}: ${p.kotlinType},
</#if>
</#list>
)
