package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiSyncKoinUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseDbOnlyDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureModuleAggregator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerTemplateRenderer
import com.dqc.egsengine.feature.scaffold.domain.ClientDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ClientPrefsScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpPreferencesScaffolder
import org.koin.dsl.module

/** KMP platform bindings. */
val scaffoldKmpModule =
    module {
        // Data generators
        single { KmpSwaggerTemplateRenderer(get()) }
        single { KmpSwaggerCodeGenerator(get(), get(), get()) }
        single { KmpCombinedRepositoryGenerator() }
        single { KmpRepositoryImplGenerator() }
        single { KmpDatabaseCodeGenerator(get()) }
        single { KmpDatabaseRepositoryGenerator(get()) }
        single { KmpDatabaseDbOnlyDataModuleUpdater() }
        single { KmpDatabaseEntityMapperGenerator(get()) }
        single { KmpDatabaseUseCaseGenerator(get()) }
        single { KmpPrefsUseCaseGenerator(get()) }
        single { KmpFeatureBuildGradleUpdater() }
        single { KmpFeatureModuleAggregator() }
        single { KmpDatabaseGeneratedDataModuleUpdater() }
        single { KmpPrefsGeneratedDataModuleUpdater() }
        single { KmpApiSyncKoinUpdater(get(), get()) }
        single { KmpModuleGenerator(settingsUpdater = get(), templateEngine = get()) }
        single { KmpApiGenerator(get()) }

        // Domain
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
            ClientDatabaseScaffolder(
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
    }
