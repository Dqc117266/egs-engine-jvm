package ${useCasePackage}

import ${domainRepositoryImport}

<#list extraImports as imp>
import ${imp}
</#list>

internal class ${useCaseName}(
    private val repository: ${combinedRepositoryName},
) {
    suspend operator fun invoke(${invokeParams}): ${returnType} {
        return repository.${repositoryCall}
    }
}
