package com.ajd.egsengine.feature.script.di

import com.ajd.egsengine.feature.script.data.ScriptLoader
import com.ajd.egsengine.feature.script.domain.ScriptEngine
import org.koin.dsl.module

val featureScriptModule = module {
    single { ScriptLoader() }
    single { ScriptEngine(get()) }
}
