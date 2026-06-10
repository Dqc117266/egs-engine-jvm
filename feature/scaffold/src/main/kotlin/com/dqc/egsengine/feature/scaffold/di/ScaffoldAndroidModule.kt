package com.dqc.egsengine.feature.scaffold.di

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
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.domain.AndroidDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.AndroidPreferencesScaffolder
import org.koin.dsl.module

/** Android platform bindings. */
val scaffoldAndroidModule =
    module {
        // Data generators
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
        single { AndroidPrefsUseCaseGenerator(get()) }
        single { AndroidApiSyncKoinUpdater() }
        single { AndroidModuleGenerator(settingsUpdater = get(), templateEngine = get()) }
        single { AndroidApiGenerator(get()) }

        // Domain
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
            AndroidPreferencesScaffolder(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
    }
