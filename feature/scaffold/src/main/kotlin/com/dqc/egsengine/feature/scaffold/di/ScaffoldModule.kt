package com.dqc.egsengine.feature.scaffold.di

import org.koin.dsl.module

val featureScaffoldModule =
    module {
        includes(
            scaffoldSharedModule,
            scaffoldKmpModule,
            scaffoldAndroidModule,
            scaffoldServerModule,
        )
    }
