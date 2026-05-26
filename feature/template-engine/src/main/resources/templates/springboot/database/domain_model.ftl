${warningGenerated}
package ${generatePackage}.domain.model
<#list domainImports as imp>
import ${imp}
</#list>

data class ${entityPascal}(
<#list columns as col>
    val ${col.kotlinName}: ${col.kotlinType}<#if col.nullable>?</#if>${col.defaultSuffix}<#if col_has_next>,</#if>
</#list>
)
