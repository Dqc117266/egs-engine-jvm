package com.example.demo.feature.task.presentation.screen

import com.example.demo.feature.base.presentation.viewmodel.BaseViewModel

internal class TaskViewModel : BaseViewModel<TaskContract.State, TaskContract.Intent, TaskContract.Effect>(
    TaskContract.State(
        isLoading = false,
        error = null,
    ),
) {
    override fun registerIntents() {
    }
}
