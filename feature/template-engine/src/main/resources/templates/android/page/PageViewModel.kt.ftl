package ${screenPkg}

import ${resultPackage}.Result
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
<#if hasUseCases>
<#list useCases as uc>
        registerIntent<${pascalName}Contract.Intent.${uc.intentName}> {
<#if uc.parameters?has_content>
            ${uc.handlerName}(<#list uc.parameters as p>it.${p.name}<#if p_has_next>, </#if></#list>)
<#else>
            ${uc.handlerName}()
</#if>
        }

</#list>
<#else>
        // TODO: Register intent handlers
</#if>
    }

<#list useCases as uc>
<#assign h = useCaseHandlers[uc_index] />
    private suspend fun ${h.handlerName}(<#list uc.parameters as p>${p.name}: ${p.kotlinType}<#if p_has_next>, </#if></#list>) {
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

</#list>
}
<#else>
internal class ${pascalName}ViewModel : androidx.lifecycle.ViewModel()
</#if>
