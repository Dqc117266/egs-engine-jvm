package com.dqc.example.feature.task.presentation.screen.taskList

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: TaskListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle effects (one-time events)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskListContract.Effect.ShowToast -> {
                    // TODO: Show Toast via SnackbarHostState
                }
            }
        }
    }

    // UI Content
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        when {
            uiState.isLoading -> {
                // TODO: Show LoadingIndicator()
            }
            uiState.error != null -> {
                // TODO: Show Error View
            }
            else -> {
                // TODO: Show Content
            }
        }
    }
}
