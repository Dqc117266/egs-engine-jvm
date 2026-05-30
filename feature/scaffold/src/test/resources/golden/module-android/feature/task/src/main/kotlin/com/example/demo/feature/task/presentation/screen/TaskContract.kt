package com.example.demo.feature.task.presentation.screen

import com.example.demo.feature.base.presentation.viewmodel.UiState
import com.example.demo.feature.base.presentation.viewmodel.UiIntent
import com.example.demo.feature.base.presentation.viewmodel.UiEffect

interface TaskContract {

    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : UiState

    sealed interface Intent : UiIntent

    sealed class Effect : UiEffect
}
