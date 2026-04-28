${warningGenerated}
package ${generatePackage}.data.mapper

import ${generatePackage}.data.datasource.cache.model.${entityPascal}CacheModel
import ${generatePackage}.domain.model.${entityPascal}

open class Generated${entityPascal}CacheMapper {
    open fun toDomain(model: ${entityPascal}CacheModel): ${entityPascal} =
        ${entityPascal}(
<#list cacheModelColumns as col>
            ${col.kotlinName} = model.${col.kotlinName}<#if col_has_next>,</#if>
</#list><#if useJpaAuditingBase>,
            createdAt = null,
            updatedAt = null</#if>
        )

    open fun toCacheModel(domain: ${entityPascal}): ${entityPascal}CacheModel =
        ${entityPascal}CacheModel(
<#list cacheModelColumns as col>
            ${col.kotlinName} = domain.${col.kotlinName}<#if col_has_next>,</#if>
</#list>
        )
}
