package ${packageName}.presentation.screen

import ${uiContractPackage}.UiState
import ${uiContractPackage}.UiIntent
import ${uiContractPackage}.UiEffect

interface ${pascal}Contract {

    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : UiState

    sealed interface Intent : UiIntent

    sealed class Effect : UiEffect
}
