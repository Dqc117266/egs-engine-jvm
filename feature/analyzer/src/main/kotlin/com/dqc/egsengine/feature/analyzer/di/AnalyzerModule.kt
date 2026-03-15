package com.dqc.egsengine.feature.analyzer.di

import com.dqc.egsengine.feature.analyzer.data.BuildFileParser
import com.dqc.egsengine.feature.analyzer.data.GradleProjectScanner
import com.dqc.egsengine.feature.analyzer.domain.ProjectAnalyzer
import org.koin.dsl.module

val featureAnalyzerModule = module {
    single { BuildFileParser() }
    single { GradleProjectScanner(get()) }
    single { ProjectAnalyzer(get()) }
}
