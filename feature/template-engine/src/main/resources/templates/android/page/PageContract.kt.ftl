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
        val ${f.name}: ${f.typeContractRef}? = null,
</#list>
    ) : UiState

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
