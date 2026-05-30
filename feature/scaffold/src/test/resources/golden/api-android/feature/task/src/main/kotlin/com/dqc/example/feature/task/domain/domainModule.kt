package com.dqc.example.feature.task.domain

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import com.dqc.example.feature.task.domain.usecase.TopicUpdateTopicUseCase

internal val domainModule = module {
    singleOf(::TopicUpdateTopicUseCase)
}
