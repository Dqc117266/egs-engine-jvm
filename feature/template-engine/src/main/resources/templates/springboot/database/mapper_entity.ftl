${warningGenerated}
package ${generatePackage}.data.mapper

import ${generatePackage}.api.dto.Create${entityPascal}Request
import ${generatePackage}.api.dto.${entityPascal}Response
import ${generatePackage}.api.dto.Update${entityPascal}Request
import ${generatePackage}.data.datasource.jpa.entity.${entityPascal}Entity
import ${generatePackage}.domain.model.${entityPascal}

open class Generated${entityPascal}EntityMapper {
    open fun toDomain(entity: ${entityPascal}Entity): ${entityPascal} =
        ${entityPascal}(
<#list columns as col>
            ${col.kotlinName} = <#if col.pk>entity.${pkProp}<#elseif useJpaAuditingBase && col.sqlName?lower_case == "created_at">entity.createdAt<#elseif useJpaAuditingBase && col.sqlName?lower_case == "updated_at">entity.updatedAt<#else>entity.${col.kotlinName}</#if><#if col_has_next>,</#if>
</#list>
        )

    open fun toNewEntity(domain: ${entityPascal}): ${entityPascal}Entity =
        ${entityPascal}Entity().apply {
<#list entityBodyColumns as col>
            ${col.kotlinName} = domain.${col.kotlinName}
</#list>
        }

    open fun toResponse(domain: ${entityPascal}): ${entityPascal}Response =
        ${entityPascal}Response(
<#list nonPkColumns as col>
            ${col.kotlinName} = domain.${col.kotlinName}<#if col_has_next>,</#if>
</#list>
        )

    open fun applyCreate(request: Create${entityPascal}Request): ${entityPascal}Entity =
        ${entityPascal}Entity().apply {
<#list businessNonPkColumns as col>
            <#if col.kotlinName == "status" && col.kotlinType == "Int">
            ${col.kotlinName} = request.${col.kotlinName} ?: 1
            <#else>
            ${col.kotlinName} = request.${col.kotlinName}
            </#if>
</#list>
        }

    open fun applyUpdate(entity: ${entityPascal}Entity, request: Update${entityPascal}Request) {
<#list businessNonPkColumns as col>
        request.${col.kotlinName}?.let { entity.${col.kotlinName} = it }
</#list>
    }
}
