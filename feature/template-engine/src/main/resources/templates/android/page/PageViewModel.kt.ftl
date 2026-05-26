package ${screenPkg}

<#list contractImports as imp>
${imp}
</#list>
<#if hasResultBasedHandler>
import ${resultPackage}.Result
</#if>
<#if hasPagedOffset>
import ${pageResultClassFqn}
</#if>
<#list useCases as uc>
import ${uc.packageName}.${uc.name}
</#list>
<#if hasBaseViewModel>
import ${baseViewModelImport}
</#if>

<#if hasBaseViewModel>
internal class ${pascalName}ViewModel(
<#list useCases as uc>
    private val ${uc.camelName}: ${uc.name},
</#list>
) : ${baseViewModelSimpleName}<${pascalName}Contract.State, ${pascalName}Contract.Intent, ${pascalName}Contract.Effect>(
    ${pascalName}Contract.State(
        isLoading = false,
        error = null,
    ),
) {

    override fun registerIntents() {
<#if hasPagedOffset || hasUseCases>
<#list intentInners as intent>
        registerIntent<${pascalName}Contract.Intent.${intent.simpleName}> {
<#if hasPagedOffset && (intent.simpleName == "Refresh" || intent.simpleName == "Retry")>
            loadPage(refresh = true)
<#elseif hasPagedOffset && intent.simpleName == "LoadMore">
            loadPage(refresh = false)
<#else>
<#list useCases as uc>
<#if uc.intentName == intent.simpleName>
<#if intent.emptyParams>
            ${uc.handlerName}()
<#else>
            ${uc.handlerName}(<#list intent.params as p>it.${p.name}<#if p_has_next>, </#if></#list>)
</#if>
</#if>
</#list>
</#if>
        }

</#list>
<#else>
        // TODO: Register intent handlers
</#if>
    }

<#if hasPagedOffset && primaryPagedUseCaseCamel?has_content>
    private fun loadPage(refresh: Boolean) {
        runPagedLoad<${pagedStateItemContractRef}>(refresh = refresh) { page, size ->
            when (val result = ${primaryPagedUseCaseCamel}(${primaryPagedArgList})) {
                is Result.Success -> {
                    val data = result.value
                    PageResult(
                        list = data.list,
                        total = data.total,
                        page = page,
                        pageSize = size,
                    )
                }
                is Result.Failure -> throw (result.throwable ?: IllegalStateException("Paging error"))
            }
        }
    }

</#if>
<#list useCases as uc>
<#assign h = useCaseHandlers[uc_index] />
<#if h.pagedBased>
<#-- offset paging handled by loadPage -->
<#elseif h.pagedFlowBased>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launch {
            ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>).collect { pagingData ->
                // Use androidx.paging.compose.collectAsLazyPagingItems(pagingData) in UI.
                updateState { copy(error = null) }
            }
        }
    }

<#elseif h.resultBased>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launchRequest(showLoading = ${h.showLoading?c}) {
<#if uc.parameters?has_content>
            when (val result = ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>)) {
<#else>
            when (val result = ${h.useCaseCamel}()) {
</#if>
                is Result.Success -> {
                    updateState { copy(${h.useCaseCamel} = result.value) }
                }
                is Result.Failure -> {
                    updateState { copy(error = result.throwable?.message) }
                }
            }
        }
    }

<#elseif h.flowBased>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launch {
<#if uc.parameters?has_content>
            ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>)
<#else>
            ${h.useCaseCamel}()
</#if>
            // TODO: collect Flow and update State
        }
    }

<#elseif h.unitEntityEchoToState>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launchRequest {
<#if uc.parameters?has_content>
            ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>)
            updateState { copy(${h.unitEchoStatePropertyName} = ${h.unitEchoParamName}, error = null) }
<#else>
            ${h.useCaseCamel}()
</#if>
        }
    }

<#elseif h.directReturnToState>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launchRequest {
<#if uc.parameters?has_content>
            val ret = ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>)
<#else>
            val ret = ${h.useCaseCamel}()
</#if>
            updateState { copy(${h.directStatePropertyName} = ret, error = null) }
        }
    }

<#else>
    private fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinTypeContractRef}<#if p_has_next>, </#if></#list>) {
        launchRequest {
<#if uc.parameters?has_content>
            ${h.useCaseCamel}(<#list uc.parameters as p>${p.name} = ${p.name}<#if p_has_next>, </#if></#list>)
<#else>
            ${h.useCaseCamel}()
</#if>
            // TODO: map result to State (or add Result return type to UseCase)
        }
    }

</#if>
</#list>
}
<#else>
internal class ${pascalName}ViewModel : androidx.lifecycle.ViewModel()
</#if>
