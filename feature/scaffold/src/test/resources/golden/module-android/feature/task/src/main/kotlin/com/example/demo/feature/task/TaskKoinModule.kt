package com.example.demo.feature.task

import org.koin.core.module.Module
import com.example.demo.feature.task.data.dataModule
import com.example.demo.feature.task.domain.domainModule
import com.example.demo.feature.task.presentation.presentationModule

val featureTaskModules: List<Module> = listOf(
    dataModule,
    domainModule,
    presentationModule,
)
