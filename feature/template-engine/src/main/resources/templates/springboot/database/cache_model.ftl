${warningGenerated}
package ${generatePackage}.data.datasource.cache.model

<#if cacheNeedsBigDecimalImport>
import java.math.BigDecimal
</#if>

/**
 * Serializable Redis payload (GenericJackson2JsonRedisSerializer compatible).
 */
data class ${entityPascal}CacheModel(
<#list cacheModelColumns as col>
    val ${col.kotlinName}: ${col.kotlinType}<#if col.nullable>?</#if><#if col_has_next>,</#if>
</#list>
)
