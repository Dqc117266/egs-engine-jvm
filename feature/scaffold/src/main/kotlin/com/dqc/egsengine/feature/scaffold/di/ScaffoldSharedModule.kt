package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.PageGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.swagger.FtlSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerTemplateRenderer
import com.dqc.egsengine.template.di.featureTemplateEngineModule
import org.koin.dsl.module

/** Shared bindings used across all platforms. */
val scaffoldSharedModule = module {
    includes(featureTemplateEngineModule)

    single { EgsConfigReader() }
    single { SettingsGradleUpdater() }
    single { SwaggerParser() }
    single { SwaggerTemplateRenderer(get()) }
    single { SwaggerCodeGenerator(get(), get(), get()) }
    single { FtlSwaggerCodeGenerator(get()) }
    single { PageGenerator(get()) }
    single { UseCaseScanner() }
    single { FeatureDiUpdater() }
    single { DdlParser() }
    single { WorkspaceConfigResolver(get()) }
}
