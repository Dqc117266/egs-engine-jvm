package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import org.koin.dsl.module

val featureScaffoldModule = module {
    single { EgsConfigReader() }
    single { ModuleGenerator() }
    single { SettingsGradleUpdater() }
    single { SwaggerParser() }
    single { SwaggerCodeGenerator() }
    single { UseCaseScanner() }
    single { FeatureDiUpdater() }
    single<ModuleScaffolder> { ModuleScaffolder(get(), get(), get()) }
    single<SwaggerApiScaffolder> { SwaggerApiScaffolder(get(), get(), get()) }
    single<PageScaffolder> { PageScaffolder(get(), get(), get()) }
}
