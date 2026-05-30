package com.dqc.example.feature.task.presentation.screen.taskList

import com.dqc.example.feature.base.presentation.viewmodel.UiState
import com.dqc.example.feature.base.presentation.viewmodel.UiIntent
import com.dqc.example.feature.base.presentation.viewmodel.UiEffect

interface TaskListContract {

    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
        val topicUpdateTopic: Boolean? = null,
    ) : UiState {
    }

    sealed interface Intent : UiIntent {
        data class TopicUpdateTopic(
            val topicId: Long
        ) : Intent
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
    }
}
