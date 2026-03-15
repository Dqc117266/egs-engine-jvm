package com.ajd.egsengine.feature.init.di

import com.ajd.egsengine.feature.init.data.BaseClassScannerImpl
import com.ajd.egsengine.feature.init.data.EgsConfigWriter
import com.ajd.egsengine.feature.init.domain.BaseClassScanner
import com.ajd.egsengine.feature.init.domain.ProjectInitializer
import org.koin.dsl.module

val featureInitModule = module {
    single<BaseClassScanner> { BaseClassScannerImpl() }
    single { EgsConfigWriter() }
    single { ProjectInitializer(get(), get()) }
}
