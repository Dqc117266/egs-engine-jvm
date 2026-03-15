package com.dqc.egsengine.feature.script.di

import com.dqc.egsengine.feature.script.data.ScriptLoader
import com.dqc.egsengine.feature.script.domain.ScriptEngine
import org.koin.dsl.module

val featureScriptModule = module {
    single { ScriptLoader() }
    single { ScriptEngine(get()) }
}
