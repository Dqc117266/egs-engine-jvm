package com.dqc.egsengine.feature.base.presentation

object DryRunFormatter {
    enum class OutputFormat { TEXT, JSON }

    fun format(
        rootPath: String,
        files: List<String>,
        format: OutputFormat = OutputFormat.TEXT,
    ): String = when (format) {
        OutputFormat.TEXT -> formatText(rootPath, files)
        OutputFormat.JSON -> formatJson(rootPath, files)
    }

    private fun formatText(
        rootPath: String,
        files: List<String>,
    ): String = buildString {
        appendLine(CliFormatter.formatInfo("[dry-run] Would write ${files.size} file(s) to $rootPath"))
        files.take(20).forEach { appendLine("  $it") }
        if (files.size > 20) {
            appendLine("  ... and ${files.size - 20} more")
        }
    }

    private fun formatJson(
        rootPath: String,
        files: List<String>,
    ): String = buildString {
        appendLine("{")
        appendLine("  \"dryRun\": true,")
        appendLine("  \"rootPath\": \"$rootPath\",")
        append("  \"files\": [")
        append(files.joinToString(",") { "\"$it\"" })
        appendLine("]")
        append("}")
    }
}
