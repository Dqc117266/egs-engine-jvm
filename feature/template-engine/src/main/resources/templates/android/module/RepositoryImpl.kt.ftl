package ${packageName}.data.repository

<#list repositoryImplImports as imp>
import ${imp}
</#list>

internal class ${pascal}RepositoryImpl(
<#if hasRetrofit>
    private val retrofitProvider: ${retrofitProviderSimpleName},
</#if>
) : ${pascal}Repository {

    override suspend fun getData(): ${repositoryReturnType} {
        TODO("Not yet implemented")
    }
}
