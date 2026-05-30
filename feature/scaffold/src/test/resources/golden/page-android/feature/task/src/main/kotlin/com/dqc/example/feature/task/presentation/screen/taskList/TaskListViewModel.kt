package com.dqc.example.feature.task.presentation.screen.taskList

import com.dqc.example.feature.base.domain.result.Result
import com.dqc.example.feature.task.domain.usecase.TopicUpdateTopicUseCase
import com.dqc.example.feature.base.presentation.viewmodel.BaseViewModel

internal class TaskListViewModel(
    private val topicUpdateTopic: TopicUpdateTopicUseCase,
) : BaseViewModel<TaskListContract.State, TaskListContract.Intent, TaskListContract.Effect>(
    TaskListContract.State(
        isLoading = false,
        error = null,
    ),
) {

    override fun registerIntents() {
        registerIntent<TaskListContract.Intent.TopicUpdateTopic> {
            handleTopicUpdateTopic(it.topicId)
        }

    }

    private fun handleTopicUpdateTopic(topicId: Long) {
        launchRequest(showLoading = true) {
            when (val result = topicUpdateTopic(topicId = topicId)) {
                is Result.Success -> {
                    updateState { copy(topicUpdateTopic = result.value) }
                }
                is Result.Failure -> {
                    updateState { copy(error = result.throwable?.message) }
                }
            }
        }
    }

}
