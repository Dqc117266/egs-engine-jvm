${warningGenerated}
package ${generatePackage}.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
<#if dtoNeedsInstantImport>
import java.time.Instant
</#if>

data class Create${entityPascal}Request(
<#list businessNonPkColumns as col>
    <#if !col.nullable && col.kotlinType == "String">
    @field:NotBlank
    </#if>
    <#if col.maxLength?? && col.kotlinType == "String">
    @field:Size(max = ${col.maxLength})
    <#elseif col.kotlinType == "String">
    @field:Size(max = 65535)
    </#if>
    <#if col.kotlinType == "Int">
    @field:Min(0)
    @field:Max(2_000_000_000)
    </#if>
    <#if col.nullable && col.kotlinType != "Int">
    val ${col.kotlinName}: ${col.kotlinType}? = null<#if col_has_next>,</#if>
    <#elseif col.kotlinType == "Int" && col.kotlinName == "status">
    val ${col.kotlinName}: Int? = 1<#if col_has_next>,</#if>
    <#else>
    val ${col.kotlinName}: ${col.kotlinType}<#if col_has_next>,</#if>
    </#if>
</#list>
)

data class Update${entityPascal}Request(
<#list businessNonPkColumns as col>
    <#if col.maxLength?? && col.kotlinType == "String">
    @field:Size(max = ${col.maxLength})
    </#if>
    <#if col.kotlinType == "Int">
    @field:Min(0)
    @field:Max(2_000_000_000)
    </#if>
    val ${col.kotlinName}: ${col.kotlinType}? = null<#if col_has_next>,</#if>
</#list>
)
