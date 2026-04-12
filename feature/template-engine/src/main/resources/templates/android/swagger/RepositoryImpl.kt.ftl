package ${packageName}

<#if needsToResult>
import ${toResultPackage}.toResult
</#if>
<#if needsToDomainImport>
import ${dataModelPackage}.toDomain
</#if>
<#if needsToDataImport>
import ${dataModelPackage}.toData
</#if>
import ${domainRepositoryPackage}.${repositoryName}
import ${servicePackage}.${serviceName}

internal class ${repositoryImplName}(
    private val service: ${serviceName},
) : ${repositoryName} {
<#list operations as op>
    override suspend fun ${op.operationId}(
<#list op.params as param>
        ${param.name}: ${param.type}<#if param_has_next>,</#if>
</#list>
<#if op.hasBody>
<#if op.params?has_content>,</#if>
        body: ${op.bodyType}
</#if>
    ): ${op.returnType} {
        ${op.statement}
    }

</#list>
}
