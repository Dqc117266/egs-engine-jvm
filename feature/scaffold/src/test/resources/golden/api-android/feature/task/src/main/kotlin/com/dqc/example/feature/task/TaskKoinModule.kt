package com.dqc.example.feature.task

import org.koin.core.module.Module
import com.dqc.example.feature.task.presentation.presentationModule
import com.dqc.example.feature.task.domain.domainModule
import com.dqc.example.feature.task.data.dataModule

val featureTaskModules: List<Module> = listOf(
    presentationModule,
    domainModule,
    dataModule,
)
