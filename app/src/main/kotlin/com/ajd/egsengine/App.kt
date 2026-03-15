package com.ajd.egsengine

import com.ajd.egsengine.di.appModule
import com.ajd.egsengine.feature.analyzer.di.featureAnalyzerModule
import com.ajd.egsengine.feature.analyzer.presentation.AnalyzeCommand
import com.ajd.egsengine.feature.command.di.featureCommandModule
import com.ajd.egsengine.feature.command.presentation.ShellCommand
import com.ajd.egsengine.feature.common.di.commonModule
import com.ajd.egsengine.feature.init.di.featureInitModule
import com.ajd.egsengine.feature.init.presentation.InitCommand
import com.ajd.egsengine.feature.scaffold.di.featureScaffoldModule
import com.ajd.egsengine.feature.scaffold.presentation.CreateCommand
import com.ajd.egsengine.feature.script.di.featureScriptModule
import com.ajd.egsengine.feature.script.presentation.ScriptCommand
import com.ajd.egsengine.feature.task.di.featureTaskModule
import com.ajd.egsengine.feature.task.presentation.TaskCommand
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
