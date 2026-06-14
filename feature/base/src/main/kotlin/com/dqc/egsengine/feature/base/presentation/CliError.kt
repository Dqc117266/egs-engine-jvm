package com.dqc.egsengine.feature.base.presentation

/**
 * 统一 CLI 错误模型。presentation 层捕获后只负责展示 + 决定 exit code，
 * 业务/domain 层抛出具体的 [CliError] 子类来表达失败原因。
 *
 * 每个 [CliError] 携带一个稳定的退出码（exit code），便于脚本判断。
 * 继承 [Exception] 以便可被 Kotlin try/catch 捕获并携带 message。
 */
sealed class CliError(message: String) : Exception(message) {
    /** 退出码：用法错误 2，功能不支持 3，其它失败 1。 */
    abstract val exitCode: Int

    /** 用户用法错误（参数缺失、路径无效、不支持的平台等）。 */
    class UsageError(message: String) : CliError(message) {
        override val exitCode: Int = EXIT_USAGE
    }

    /** 代码生成失败（模板渲染、文件写入、包名改写等）。 */
    class GenerationError(message: String) : CliError(message) {
        override val exitCode: Int = EXIT_FAILURE
    }

    /** 功能尚未实现或不支持（如 SpringBoot/Vue3 Swagger 生成器）。 */
    class UnsupportedFeature(message: String) : CliError(message) {
        override val exitCode: Int = EXIT_UNSUPPORTED
    }

    /** 其它未预期错误。 */
    class GenericError(message: String) : CliError(message) {
        override val exitCode: Int = EXIT_FAILURE
    }

    private companion object {
        const val EXIT_FAILURE = 1
        const val EXIT_USAGE = 2
        const val EXIT_UNSUPPORTED = 3
    }
}
