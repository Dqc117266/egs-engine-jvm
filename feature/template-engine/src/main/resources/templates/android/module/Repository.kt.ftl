package ${packageName}.domain.repository

<#list repositoryImports as imp>
import ${imp}
</#list>

internal interface ${pascal}Repository {
    suspend fun getData(): ${repositoryReturnType}
}
