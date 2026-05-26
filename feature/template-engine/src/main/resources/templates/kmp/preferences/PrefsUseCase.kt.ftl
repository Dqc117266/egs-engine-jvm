package ${useCasePackage}

import ${domainRepositoryImport}
<#if needsFlowImport>
import kotlinx.coroutines.flow.Flow
</#if>
<#list extraImports as imp>
import ${imp}
</#list>

internal class ${useCaseName}(
    private val repository: ${combinedRepositoryName},
) {
<#if suspendInvoke>
    suspend operator fun invoke(${invokeParams}): ${returnType} {
        return repository.${repositoryCall}
    }
<#else>
    operator fun invoke(${invokeParams}): ${returnType} {
        return repository.${repositoryCall}
    }
</#if>
}
