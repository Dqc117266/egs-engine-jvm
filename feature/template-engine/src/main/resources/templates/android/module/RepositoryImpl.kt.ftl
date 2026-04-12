package ${packageName}.data.repository

/*
 * egs-codegen: scaffold-repository-impl ¡ª after `client api sync` this class extends Generated¡­RepositorySupport.
 * Add // egs-sync:freeze on its own line to prevent api sync from overwriting this file.
 */
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
