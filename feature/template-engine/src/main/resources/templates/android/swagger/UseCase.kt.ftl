package ${packageName}

<#list imports as imp>
import ${imp}
</#list>

internal class ${useCaseName}(
    private val repository: ${repositoryName},
) {
<#if !params?has_content && hasBody>
    suspend operator fun invoke(body: ${bodyType}): ${returnType} {
        return repository.${operationId}(${callArgs})
    }
<#else>
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
</#if>
}
