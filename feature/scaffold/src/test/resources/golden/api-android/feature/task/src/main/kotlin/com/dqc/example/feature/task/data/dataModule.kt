package com.dqc.example.feature.task.data

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import retrofit2.Retrofit
import com.dqc.example.feature.task.data.repository.TaskRepositoryImpl
import com.dqc.example.feature.task.domain.repository.TaskRepository
import com.dqc.example.feature.task.data.datasource.api.service.TaskRetrofitService

internal val dataModule = module {
    singleOf(::TaskRepositoryImpl) { bind<TaskRepository>() }
    single { get<Retrofit>().create(TaskRetrofitService::class.java) }
}
