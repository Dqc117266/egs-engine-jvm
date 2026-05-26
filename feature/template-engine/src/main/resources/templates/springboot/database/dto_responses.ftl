${warningGenerated}
package ${generatePackage}.api.dto

<#if responseNeedsInstantImport>
import java.time.Instant
</#if>

data class ${entityPascal}Response(
<#-- Include PK so REST list/detail JSON exposes ids (admin tables, linking, deletes). -->
<#list columns as col>
    val ${col.kotlinName}: ${col.kotlinType}<#if col.nullable>?</#if><#if col_has_next>,</#if>
</#list>
)
