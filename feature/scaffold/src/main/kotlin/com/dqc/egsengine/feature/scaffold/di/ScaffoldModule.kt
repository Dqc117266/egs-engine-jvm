package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.EntityScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import org.koin.dsl.module

val featureScaffoldModule = module {
    // Data layer - shared
    single { EgsConfigReader() }
    single { ModuleGenerator() }
    single { SettingsGradleUpdater() }
    single { SwaggerParser() }
    single { SwaggerCodeGenerator() }
    single { UseCaseScanner() }
    single { FeatureDiUpdater() }
    single { DdlParser() }

    // Data layer - workspace config
    single { WorkspaceConfigResolver(get()) }

    // Platform module generators
    single { AndroidModuleGenerator(get()) }
    single { SpringBootModuleGenerator(get()) }
    single { Vue3ModuleGenerator() }

    single<Map<Platform, PlatformModuleGenerator>> {
        mapOf(
            Platform.ANDROID to get<AndroidModuleGenerator>(),
            Platform.KMP to get<AndroidModuleGenerator>(),
            Platform.KMP_ANDROID to get<AndroidModuleGenerator>(),
            Platform.SPRING_BOOT to get<SpringBootModuleGenerator>(),
            Platform.VUE3 to get<Vue3ModuleGenerator>(),
        )
    }

    // Platform API generators
    single { AndroidApiGenerator(get()) }
    single { SpringBootApiGenerator() }
    single { Vue3ApiGenerator() }

    single<Map<Platform, PlatformApiGenerator>> {
        mapOf(
            Platform.ANDROID to get<AndroidApiGenerator>(),
            Platform.KMP to get<AndroidApiGenerator>(),
            Platform.KMP_ANDROID to get<AndroidApiGenerator>(),
            Platform.SPRING_BOOT to get<SpringBootApiGenerator>(),
            Platform.VUE3 to get<Vue3ApiGenerator>(),
        )
    }

    // Spring Boot specific
    single { SpringBootCrudGenerator() }

    // Domain layer
    single { ModuleScaffolder(get(), get(), get(), get(), get()) }
    single { SwaggerApiScaffolder(get(), get(), get()) }
    single { PageScaffolder(get(), get(), get()) }
    single { ApiSyncScaffolder(get(), get(), get()) }
    single { EntityScaffolder(get(), get(), get()) }
}
