package com.dqc.egsengine

import com.dqc.egsengine.di.appModule
import com.dqc.egsengine.feature.analyzer.di.featureAnalyzerModule
import com.dqc.egsengine.feature.analyzer.presentation.AnalyzeCommand
import com.dqc.egsengine.feature.command.di.featureCommandModule
import com.dqc.egsengine.feature.command.presentation.ShellCommand
import com.dqc.egsengine.feature.common.di.commonModule
import com.dqc.egsengine.feature.init.di.featureInitModule
import com.dqc.egsengine.feature.init.presentation.InitCommand
import com.dqc.egsengine.feature.scaffold.di.featureScaffoldModule
import com.dqc.egsengine.feature.scaffold.presentation.CreateCommand
import com.dqc.egsengine.feature.script.di.featureScriptModule
import com.dqc.egsengine.feature.script.presentation.ScriptCommand
import com.dqc.egsengine.feature.task.di.featureTaskModule
import com.dqc.egsengine.feature.task.presentation.TaskCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class EgsEngineCli : CliktCommand(name = "egs-engine") {
    private val version by option("--version", "-v").flag()

    override fun run() {
        if (version) {
            echo("egs-engine v${AppVersion.NAME}")
        }
    }
}

fun main(args: Array<String>) {
    initKoin()

    EgsEngineCli()
        .subcommands(
            AnalyzeCommand(),
            ShellCommand(),
            TaskCommand(),
            ScriptCommand(),
            InitCommand(),
            CreateCommand.withSubcommands(),
        )
        .main(args)
}

private fun initKoin() {
    GlobalContext.startKoin {
        printLogger(Level.INFO)

        modules(
            appModule,
            commonModule,
            featureCommandModule,
            featureTaskModule,
            featureScriptModule,
            featureAnalyzerModule,
            featureInitModule,
            featureScaffoldModule,
        )
    }
}
