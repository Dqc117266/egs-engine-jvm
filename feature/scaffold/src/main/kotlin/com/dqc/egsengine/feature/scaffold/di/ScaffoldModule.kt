package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.ClientAppNavigationWiring
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiDbRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiSyncKoinUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseDbOnlyDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDbOnlyRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidPrefsGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiSyncKoinUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseDbOnlyDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootAppDependencyUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootGeneratedPathsManifest
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootHandWrittenShellGenerator
import com.dqc.egsengine.feature.scaffold.domain.AdminVueCrudScaffolder
import com.dqc.egsengine.feature.scaffold.domain.SpringBootDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerTemplateRenderer
import com.dqc.egsengine.template.di.featureTemplateEngineModule
import com.dqc.egsengine.feature.scaffold.domain.AndroidDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.AndroidPreferencesScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ClientDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ClientPrefsScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpPreferencesScaffolder
import com.dqc.egsengine.feature.scaffold.domain.EntityScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.CreateUseCaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ViewModelEditScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureScaffoldModule = module {
    includes(featureTemplateEngineModule)

    // Data layer - shared
    single { EgsConfigReader() }
    single { SettingsGradleUpdater() }
    single { SwaggerParser() }
    single { SwaggerTemplateRenderer(get()) }
    single { SwaggerCodeGenerator(get(), get(), get()) }
    single { KmpSwaggerTemplateRenderer(get()) }
    single { KmpCombinedRepositoryGenerator() }
    single { KmpRepositoryImplGenerator() }
    single { AndroidDatabaseCodeGenerator(get()) }
    single { AndroidDatabaseRepositoryGenerator(get()) }
    single { AndroidDatabaseGeneratedDataModuleUpdater() }
    single { AndroidDatabaseDbOnlyDataModuleUpdater() }
    single { AndroidDatabaseEntityMapperGenerator(get()) }
    single { AndroidDatabaseUseCaseGenerator(get()) }
    single { AndroidCombinedRepositoryGenerator() }
    single { AndroidDbOnlyRepositoryImplGenerator() }
    single { AndroidApiDbRepositoryImplGenerator() }
    single { AndroidPrefsGeneratedDataModuleUpdater() }
    single { AndroidFeatureBuildGradleUpdater() }
    single { KmpSwaggerCodeGenerator(get(), get(), get()) }
    single { KmpDatabaseCodeGenerator(get()) }
    single { KmpDatabaseRepositoryGenerator(get()) }
    single { KmpDatabaseDbOnlyDataModuleUpdater() }
    single { KmpDatabaseEntityMapperGenerator(get()) }
    single { KmpDatabaseUseCaseGenerator(get()) }
    single { KmpPrefsUseCaseGenerator(get()) }
    single { AndroidPrefsUseCaseGenerator(get()) }
    single { KmpFeatureBuildGradleUpdater() }
    single { KmpDatabaseGeneratedDataModuleUpdater() }
    single { KmpPrefsGeneratedDataModuleUpdater() }
    single { KmpApiSyncKoinUpdater(get(), get()) }
    single { AndroidApiSyncKoinUpdater() }
    single { UseCaseScanner() }
    single { FeatureDiUpdater() }
    single { DdlParser() }

    // Data layer - workspace config
    single { WorkspaceConfigResolver(get()) }

    // Platform module generators
    single { AndroidModuleGenerator(settingsUpdater = get(), templateEngine = get()) }
    single { KmpModuleGenerator(settingsUpdater = get(), templateEngine = get()) }
    single { ModuleGenerator(androidModuleGenerator = get()) }
    single { SpringBootModuleGenerator(get(), get()) }
    single { Vue3ModuleGenerator() }

    single<Map<Platform, PlatformModuleGenerator>>(named("platformModuleGenerators")) {
        mapOf(
            Platform.ANDROID to get<AndroidModuleGenerator>(),
            Platform.KMP to get<KmpModuleGenerator>(),
            Platform.KMP_ANDROID to get<KmpModuleGenerator>(),
            Platform.SPRING_BOOT to get<SpringBootModuleGenerator>(),
            Platform.VUE3 to get<Vue3ModuleGenerator>(),
        )
    }

    // Platform API generators
    single { AndroidApiGenerator(get()) }
    single { KmpApiGenerator(get()) }
    single { SpringBootApiGenerator() }
    single { Vue3ApiGenerator() }

    single<Map<Platform, PlatformApiGenerator>>(named("platformApiGenerators")) {
        mapOf(
            Platform.ANDROID to get<AndroidApiGenerator>(),
            Platform.KMP to get<KmpApiGenerator>(),
            Platform.KMP_ANDROID to get<KmpApiGenerator>(),
            Platform.SPRING_BOOT to get<SpringBootApiGenerator>(),
            Platform.VUE3 to get<Vue3ApiGenerator>(),
        )
    }

    // Spring Boot specific
    single { SpringBootCrudGenerator(get()) }
    single { SpringBootGeneratedPathsManifest() }
    single { SpringBootHandWrittenShellGenerator() }
    single { SpringBootAppDependencyUpdater() }
    single { AdminVueCrudScaffolder(get(), get()) }
    single {
        SpringBootDatabaseScaffolder(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    // Domain layer
    single { ModuleScaffolder(get(), get(), get(), get(), get(named("platformModuleGenerators"))) }
    single { SwaggerApiScaffolder(get(), get(), get()) }
    single { ClientAppNavigationWiring() }
    single { CreateUseCaseScaffolder(get()) }
    single { PageScaffolder(get(), get(), get(), get(), get()) }
    single { ViewModelEditScaffolder(get(), get(), get()) }
    single {
        ApiSyncScaffolder(
            workspaceResolver = get(),
            swaggerParser = get(),
            platformApiGenerators = get(named("platformApiGenerators")),
            platformModuleGenerators = get(named("platformModuleGenerators")),
            kmpApiSyncKoinUpdater = get(),
            androidApiSyncKoinUpdater = get(),
        )
    }
    single {
        KmpDatabaseScaffolder(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        AndroidDatabaseScaffolder(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        ClientDatabaseScaffolder(
            get(),
            get(),
            get(),
        )
    }
    single {
        KmpPreferencesScaffolder(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        AndroidPreferencesScaffolder(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        ClientPrefsScaffolder(
            get(),
            get(),
            get(),
        )
    }
    single { EntityScaffolder(get()) }
}
