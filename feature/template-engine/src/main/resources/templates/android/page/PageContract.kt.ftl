package ${screenPkg}

<#list contractImports as imp>
${imp}
</#list>
import ${uiContractPackage}.UiState
import ${uiContractPackage}.UiIntent
import ${uiContractPackage}.UiEffect

interface ${pascalName}Contract {

    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
<#list stateFields as f>
<#if hasPagedOffset && (f.name == "items" || f.name == "total" || f.name == "page" || f.name == "pageSize" || f.name == "isRefreshing" || f.name == "isLoadingMore" || f.name == "endReached" || f.name == "pagingError")>
<#if f.nullable>
        override val ${f.name}: ${f.typeContractRef}? = null,
<#else>
        override val ${f.name}: ${f.typeContractRef} = ${f.defaultLiteral},
</#if>
<#else>
<#if f.nullable>
        val ${f.name}: ${f.typeContractRef}? = null,
<#else>
        val ${f.name}: ${f.typeContractRef} = ${f.defaultLiteral},
</#if>
</#if>
</#list>
    ) : UiState<#if hasPagedOffset>, PagingListState<${pagedStateItemContractRef}></#if> {
<#if hasPagedOffset>

        override fun copyPaging(
            items: List<${pagedStateItemContractRef}>,
            total: Long,
            page: Int,
            pageSize: Int,
            isRefreshing: Boolean,
            isLoadingMore: Boolean,
            endReached: Boolean,
            pagingError: Throwable?,
        ): ${pascalName}Contract.State = copy(
            items = items,
            total = total,
            page = page,
            pageSize = pageSize,
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            endReached = endReached,
            pagingError = pagingError,
        )
</#if>
    }

    sealed interface Intent : UiIntent {
<#list intentInners as intent>
<#if intent.emptyParams>
        data object ${intent.simpleName} : Intent
<#else>
        data class ${intent.simpleName}(
<#list intent.params as p>
            val ${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>,</#if>
</#list>
        ) : Intent
</#if>
</#list>
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
    }
}
