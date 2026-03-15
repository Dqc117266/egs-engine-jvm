package com.dqc.egsengine.feature.common.di

import com.dqc.egsengine.feature.common.data.ConfigRepository
import com.dqc.egsengine.feature.common.data.ConfigRepositoryImpl
import org.koin.dsl.module

val commonModule = module {
    single<ConfigRepository> { ConfigRepositoryImpl() }
}
