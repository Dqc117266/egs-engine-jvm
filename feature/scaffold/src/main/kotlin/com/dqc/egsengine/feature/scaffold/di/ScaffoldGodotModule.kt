package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotGeneratorContractReader
import com.dqc.egsengine.feature.scaffold.domain.GodotCodeScaffolder
import com.dqc.egsengine.feature.scaffold.domain.GodotGameScaffolder
import org.koin.dsl.module

/** Koin bindings for the Godot platform (contract-driven entity generation + new-game scaffolding). */
val scaffoldGodotModule =
    module {
        single { GodotGeneratorContractReader() }
        single { GodotCodeGenerator(get(), get()) }
        single { GodotCodeScaffolder(get(), get(), get()) }
        single { GodotGameScaffolder(get()) }
    }
