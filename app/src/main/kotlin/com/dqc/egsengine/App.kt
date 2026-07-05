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
import com.dqc.egsengine.feature.scaffold.presentation.BackendCommand
import com.dqc.egsengine.feature.scaffold.presentation.ClientCommand
import com.dqc.egsengine.feature.scaffold.presentation.CreateCommand
import com.dqc.egsengine.feature.scaffold.presentation.FlowCommand
import com.dqc.egsengine.feature.scaffold.presentation.GameCommand
import com.dqc.egsengine.feature.scaffold.presentation.LintCommand
import com.dqc.egsengine.feature.scaffold.presentation.NewCommand
import com.dqc.egsengine.feature.scaffold.presentation.TemplateCommand
import com.dqc.egsengine.feature.scaffold.presentation.WebCommand
import com.dqc.egsengine.feature.script.di.featureScriptModule
import com.dqc.egsengine.feature.script.presentation.ScriptCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

class EgsEngineCli : CliktCommand(name = "egs-engine") {
    private val version by option("--version", "-v").flag()

    override fun commandHelp(context: com.github.ajalt.clikt.core.Context): String = "Multi-platform code scaffolding engine" +
        " for Android, KMP, Spring Boot, Vue3, and Godot"

    override fun commandHelpEpilog(context: com.github.ajalt.clikt.core.Context): String =
        """
        |Examples:
        |  egs-engine new project myapp --package com.example.myapp
        |  egs-engine new game mygame --engine godot --template base
        |  egs-engine game add enemy slime --project ./mygame
        |  egs-engine create module -m myfeature -p .
        |  egs-engine client gen database -m myfeature --ddl schema.sql -p .
        |  egs-engine analyze -p .
        |  egs-engine command -- echo hello
        |
        |Run 'egs-engine <command> --help' for detailed usage.
        """.trimMargin()

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
            ScriptCommand(),
            InitCommand(),
            CreateCommand.withSubcommands(),
            FlowCommand.withSubcommands(),
            NewCommand.withSubcommands(),
            GameCommand.withSubcommands(),
            BackendCommand.withSubcommands(),
            ClientCommand.withSubcommands(),
            WebCommand.withSubcommands(),
            LintCommand.withSubcommands(),
            TemplateCommand.withSubcommands(),
        ).main(args)
}

private fun initKoin() {
    GlobalContext.startKoin {
        // 默认静默 Koin 启动日志（P0：--help 不应打印 [INFO] [Koin] Started...）。
        // 设 EGS_DEBUG=true 时才开启 INFO 级别，便于调试 DI 问题。
        if (System.getenv("EGS_DEBUG")?.equals("true", ignoreCase = true) == true) {
            printLogger(Level.INFO)
        } else {
            printLogger(Level.NONE)
        }

        modules(
            appModule,
            commonModule,
            featureCommandModule,
            featureScriptModule,
            featureAnalyzerModule,
            featureInitModule,
            featureScaffoldModule,
        )
    }
}
