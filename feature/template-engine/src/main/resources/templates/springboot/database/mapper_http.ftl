${warningGenerated}
package ${generatePackage}.data.mapper

import ${generatePackage}.data.datasource.httpclient.model.${entityPascal}RemoteItemHttpModel
import ${generatePackage}.domain.model.${entityPascal}

open class Generated${entityPascal}HttpMapper {
    open fun toDomain(remote: ${entityPascal}RemoteItemHttpModel): ${entityPascal}? {
        val id = remote.id ?: return null
<#if hasNameStringColumn>
        val nm = remote.name ?: return null
</#if>
        return ${entityPascal}(
            id = id,
<#list businessNonPkColumns as col>
<#if hasNameStringColumn && col.kotlinName == "name">
            name = nm,
<#elseif col.kotlinType == "Int">
            ${col.kotlinName} = remote.${col.kotlinName} ?: 1,
<#elseif col.nullable>
            ${col.kotlinName} = remote.${col.kotlinName},
<#elseif col.kotlinType == "String">
            ${col.kotlinName} = remote.${col.kotlinName} ?: "",
<#else>
            ${col.kotlinName} = remote.${col.kotlinName}!!,
</#if>
</#list>
<#if useJpaAuditingBase>
            createdAt = null,
            updatedAt = null,
</#if>
        )
    }
}
