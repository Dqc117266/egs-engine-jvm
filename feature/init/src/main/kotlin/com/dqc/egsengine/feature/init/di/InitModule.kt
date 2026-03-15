package com.dqc.egsengine.feature.init.di

import com.dqc.egsengine.feature.init.data.BaseClassScannerImpl
import com.dqc.egsengine.feature.init.data.EgsConfigWriter
import com.dqc.egsengine.feature.init.domain.BaseClassScanner
import com.dqc.egsengine.feature.init.domain.ProjectInitializer
import org.koin.dsl.module

val featureInitModule = module {
    single<BaseClassScanner> { BaseClassScannerImpl() }
    single { EgsConfigWriter() }
    single { ProjectInitializer(get(), get()) }
}
