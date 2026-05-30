package com.example.demo.feature.task.data

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import com.example.demo.feature.task.data.repository.TaskRepositoryImpl
import com.example.demo.feature.task.domain.repository.TaskRepository

/**
 * Hand-written data layer wiring. Retrofit API service is provided by `generate.di.generatedDataModule`
 * after `client api sync`; ensure the root feature module includes `generatedDataModule` (see updater).
 */
internal val dataModule = module {

    singleOf(::TaskRepositoryImpl) { bind<TaskRepository>() }
}
