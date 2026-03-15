package com.ajd.egsengine.feature.task.di

import com.ajd.egsengine.feature.task.data.TaskRepository
import com.ajd.egsengine.feature.task.data.TaskRepositoryImpl
import com.ajd.egsengine.feature.task.domain.TaskScheduler
import org.koin.dsl.module

val featureTaskModule = module {
    single<TaskRepository> { TaskRepositoryImpl() }
    single { TaskScheduler(get()) }
}
