package ${packageName}

import ${repositoryPackage}.${repositoryName}

internal class ${useCaseName}(
    private val repository: ${repositoryName},
) {
    suspend operator fun invoke(
<#list params as param>
        ${param.name}: ${param.type}<#if param_has_next>,</#if>
</#list>
<#if hasBody>
<#if params?has_content>,</#if>
        body: ${bodyType}
</#if>
    ): ${returnType} {
        return repository.${operationId}(${callArgs})
    }
}
