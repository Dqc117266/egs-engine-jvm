package com.example.demo.feature.task.presentation

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf
import com.example.demo.feature.task.presentation.screen.TaskViewModel

internal val presentationModule = module {
    viewModelOf(::TaskViewModel)
}
