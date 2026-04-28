${warningGenerated}
package ${generatePackage}.api.dto

<#if responseNeedsInstantImport>
import java.time.Instant
</#if>

data class ${entityPascal}Response(
<#list nonPkColumns as col>
    val ${col.kotlinName}: ${col.kotlinType}<#if col.nullable>?</#if><#if col_has_next>,</#if>
</#list>
)
