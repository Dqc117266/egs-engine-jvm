package com.dqc.egsengine.feature.command.di

import com.dqc.egsengine.feature.base.command.CommandExecutor
import com.dqc.egsengine.feature.command.data.ShellCommandRunner
import com.dqc.egsengine.feature.command.domain.CommandService
import org.koin.dsl.module

val featureCommandModule = module {
    single { CommandExecutor() }
    single { ShellCommandRunner(get()) }
    single { CommandService(get()) }
}
