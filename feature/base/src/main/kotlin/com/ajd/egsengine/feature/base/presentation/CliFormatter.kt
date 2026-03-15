package com.ajd.egsengine.feature.base.presentation

object CliFormatter {

    fun formatTable(
        headers: List<String>,
        rows: List<List<String>>,
    ): String {
        if (headers.isEmpty()) return ""

        val columnWidths = headers.indices.map { col ->
            maxOf(
                headers[col].length,
                rows.maxOfOrNull { it.getOrElse(col) { "" }.length } ?: 0,
            )
        }

        val sb = StringBuilder()

        sb.appendLine(
            headers.mapIndexed { i, h -> h.padEnd(columnWidths[i]) }.joinToString(" | "),
        )

        sb.appendLine(
            columnWidths.joinToString("-+-") { "-".repeat(it) },
        )

        rows.forEach { row ->
            sb.appendLine(
                row.mapIndexed { i, cell -> cell.padEnd(columnWidths[i]) }.joinToString(" | "),
            )
        }

        return sb.toString()
    }

    fun formatSuccess(message: String): String = "[OK] $message"

    fun formatError(message: String): String = "[ERROR] $message"

    fun formatWarning(message: String): String = "[WARN] $message"

    fun formatInfo(message: String): String = "[INFO] $message"
}
