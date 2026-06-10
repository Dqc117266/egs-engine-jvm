package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.ClientAppNavigationWiring
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootAppDependencyUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootGeneratedPathsManifest
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootHandWrittenShellGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.vue3.Vue3ModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.AdminVueCrudScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.CreateUseCaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.EntityScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.SpringBootDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ViewModelEditScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Server (Spring Boot, Vue3) and shared domain bindings. */
val scaffoldServerModule =
    module {
        // Platform module/API generators (server side)
        single { SpringBootModuleGenerator(get(), get()) }
        single { Vue3ModuleGenerator() }
        single { ModuleGenerator(androidModuleGenerator = get()) }

        single<Map<Platform, PlatformModuleGenerator>>(named("platformModuleGenerators")) {
            mapOf(
                Platform.ANDROID to get<com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator>(),
                Platform.KMP to get<com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpModuleGenerator>(),
                Platform.KMP_ANDROID to get<com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpModuleGenerator>(),
                Platform.SPRING_BOOT to get<SpringBootModuleGenerator>(),
                Platform.VUE3 to get<Vue3ModuleGenerator>(),
            )
        }

        single { SpringBootApiGenerator() }
        single { Vue3ApiGenerator() }

        single<Map<Platform, PlatformApiGenerator>>(named("platformApiGenerators")) {
            mapOf(
                Platform.ANDROID to get<com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiGenerator>(),
                Platform.KMP to get<com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiGenerator>(),
                Platform.KMP_ANDROID to get<com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiGenerator>(),
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

        // Shared domain layer
        single { ClientAppNavigationWiring() }
        single { ModuleScaffolder(get(), get(), get(), get(), get(named("platformModuleGenerators"))) }
        single { SwaggerApiScaffolder(get(), get(), get(), get()) }
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
        single { EntityScaffolder(get()) }
    }
