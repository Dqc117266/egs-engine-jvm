package ${packageName}

<#if imports?has_content>
<#list imports as imp>
import ${imp}
</#list>

</#if>
data class ${className}(
<#list props as p>
<#if p.nullable>
    val ${p.name}: ${p.kotlinType}? = null,
<#else>
    val ${p.name}: ${p.kotlinType},
</#if>
</#list>
)
