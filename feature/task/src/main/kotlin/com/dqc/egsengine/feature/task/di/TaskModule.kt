package com.dqc.egsengine.feature.task.di

import com.dqc.egsengine.feature.task.data.TaskRepository
import com.dqc.egsengine.feature.task.data.TaskRepositoryImpl
import com.dqc.egsengine.feature.task.domain.TaskScheduler
import org.koin.dsl.module

val featureTaskModule = module {
    single<TaskRepository> { TaskRepositoryImpl() }
    single { TaskScheduler(get()) }
}
