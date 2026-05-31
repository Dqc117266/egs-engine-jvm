${warningGenerated}
package ${generatePackage}.data.datasource.httpclient.model

<#if cacheNeedsBigDecimalImport>
import java.math.BigDecimal
</#if>
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ${entityPascal}RemoteItemHttpModel(
<#list cacheModelColumns as col>
    val ${col.kotlinName}: ${col.kotlinType}? = null<#if col_has_next>,</#if>
</#list>
)
