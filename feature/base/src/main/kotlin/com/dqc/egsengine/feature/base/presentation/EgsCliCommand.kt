package com.dqc.egsengine.feature.base.presentation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import org.koin.core.component.KoinComponent

/**
 * Base class for all egs-engine CLI commands.
 *
 * Uniform error handling (P0: failures must return non-zero exit code):
 * - Command logic goes in [runCommand]; throw [CliError] for expected failures.
 * - Any uncaught exception is caught by [run], printed to stderr, and exits non-zero.
 *
 * Subclasses pass [help] to provide a one-line description shown in `--help`.
 */
abstract class EgsCliCommand(
    name: String,
    private val help: String = "",
) : CliktCommand(name = name),
    KoinComponent {

    /** Subclass implements actual command logic. Throw [CliError] for expected failures. */
    protected abstract fun runCommand()

    override fun commandHelp(context: Context): String = help

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    final override fun run() {
        try {
            runCommand()
        } catch (e: CliError) {
            echo(CliFormatter.formatError(e.message ?: "error"), err = true)
            throw ProgramResult(e.exitCode)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "invalid argument"), err = true)
            throw ProgramResult(CliError.UsageError(e.message ?: "invalid argument").exitCode)
        } catch (e: Throwable) {
            echo(CliFormatter.formatError(e.message ?: e.javaClass.simpleName), err = true)
            if (isVerbose()) {
                echo(e.stackTraceToString(), err = true)
            }
            throw ProgramResult(CliError.GenericError(e.message ?: "unknown error").exitCode)
        }
    }

    /** Override to control stack trace printing (default follows --verbose / EGS_DEBUG). */
    protected open fun isVerbose(): Boolean = false
}
