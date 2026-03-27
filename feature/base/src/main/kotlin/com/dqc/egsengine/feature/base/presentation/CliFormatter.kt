package com.dqc.egsengine.feature.base.presentation

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

    // ANSI 颜色码
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_RED = "\u001B[31m"
    private const val ANSI_YELLOW = "\u001B[33m"
    private const val ANSI_CYAN = "\u001B[36m"
    private const val ANSI_RESET = "\u001B[0m"

    fun formatSuccess(message: String): String = "$ANSI_GREEN[OK] $message$ANSI_RESET"

    fun formatError(message: String): String = "$ANSI_RED[ERROR] $message$ANSI_RESET"

    fun formatWarning(message: String): String = "$ANSI_YELLOW[WARN] $message$ANSI_RESET"

    fun formatInfo(message: String): String = "$ANSI_CYAN[INFO] $message$ANSI_RESET"

    fun green(text: String): String = "$ANSI_GREEN$text$ANSI_RESET"
}
