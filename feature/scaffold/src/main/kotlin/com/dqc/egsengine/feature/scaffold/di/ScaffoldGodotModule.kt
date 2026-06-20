package com.dqc.egsengine.feature.scaffold.di

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityRegistryUpdater
import com.dqc.egsengine.feature.scaffold.domain.GodotEntityScaffolder
import com.dqc.egsengine.feature.scaffold.domain.GodotGameScaffolder
import org.koin.dsl.module

/** Koin bindings for the Godot platform (entity generation + new-game scaffolding). */
val scaffoldGodotModule =
    module {
        single { GodotEntityGenerator(get()) }
        single { GodotEntityRegistryUpdater() }
        single { GodotEntityScaffolder(get(), get(), get()) }
        single { GodotGameScaffolder(get()) }
    }
