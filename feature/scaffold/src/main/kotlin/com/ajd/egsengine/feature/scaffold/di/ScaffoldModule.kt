package com.ajd.egsengine.feature.scaffold.di

import com.ajd.egsengine.feature.scaffold.data.EgsConfigReader
import com.ajd.egsengine.feature.scaffold.data.ModuleGenerator
import com.ajd.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.ajd.egsengine.feature.scaffold.domain.ModuleScaffolder
import org.koin.dsl.module

val featureScaffoldModule = module {
    single { EgsConfigReader() }
    single { ModuleGenerator() }
    single { SettingsGradleUpdater() }
    single { ModuleScaffolder(get(), get(), get()) }
}
