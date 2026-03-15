package com.ajd.egsengine.feature.common.di

import com.ajd.egsengine.feature.common.data.ConfigRepository
import com.ajd.egsengine.feature.common.data.ConfigRepositoryImpl
import org.koin.dsl.module

val commonModule = module {
    single<ConfigRepository> { ConfigRepositoryImpl() }
}
