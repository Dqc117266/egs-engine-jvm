package org.mifos.feature.task.generate.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.mifos.feature.task.domain.usecase.TopicUpdateTopicUseCase

internal val generatedDomainModule = module {
    // egs-gen:swagger-usecases-begin
    singleOf(::TopicUpdateTopicUseCase)
    // egs-gen:swagger-usecases-end

    // egs-gen:db-usecases-begin
    // egs-gen:db-usecases-end

    // egs-gen:prefs-usecases-begin
    // egs-gen:prefs-usecases-end
}
