package com.dqc.egsengine.feature.base.presentation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import org.koin.core.component.KoinComponent

/**
 * 所有 egs-engine CLI 命令的基类。
 *
 * 统一错误处理（P0：失败必须返回非 0）：
 * - 命令实现写在 [runCommand] 中，抛出 [CliError] 表达预期失败；
 * - 任何未捕获异常都会被 [run] 捕获，打印到 stderr 并以非零退出码终止，
 *   避免旧的"catch 后 echo 但进程 exit 0"行为。
 *
 * 子类用 `@CliktCommand(...)` 注解提供 name/help，并实现 [runCommand]。
 */
abstract class EgsCliCommand : CliktCommand(), KoinComponent {

    /** 子类实现的实际命令逻辑。抛 [CliError] 表达预期失败。 */
    protected abstract fun runCommand()

    final override fun run() {
        try {
            runCommand()
        } catch (e: CliError) {
            echo(CliFormatter.formatError(e.message ?: "error"), err = true)
            throw ProgramResult(e.exitCode)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 协程取消不应被当作普通错误吞掉
            throw e
        } catch (e: Throwable) {
            echo(CliFormatter.formatError(e.message ?: e.javaClass.simpleName), err = true)
            if (isVerbose()) {
                echo(e.stackTraceToString(), err = true)
            }
            throw ProgramResult(CliError.GenericError(e.message ?: "unknown error").exitCode)
        }
    }

    /** 子类可覆盖以控制是否打印堆栈（默认跟随 --verbose / EGS_DEBUG）。 */
    protected open fun isVerbose(): Boolean = false
}
