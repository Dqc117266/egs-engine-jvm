package com.ajd.egsengine.feature.command.di

import com.ajd.egsengine.feature.base.command.CommandExecutor
import com.ajd.egsengine.feature.command.data.ShellCommandRunner
import com.ajd.egsengine.feature.command.domain.CommandService
import org.koin.dsl.module

val featureCommandModule = module {
    single { CommandExecutor() }
    single { ShellCommandRunner(get()) }
    single { CommandService(get()) }
}
