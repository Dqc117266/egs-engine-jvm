package com.dqc.egsengine.feature.scaffold.golden

import java.io.File

/**
 * Compares generator output against a committed golden tree under
 * `src/test/resources/golden/<case>/`.
 *
 * Verify mode (default): every generated file must byte-match its golden counterpart.
 * Any mismatch, extra, or stale file prints a compact diff and fails the test.
 *
 * Update mode: pass `-Degs.golden.update=true` (or env `UPDATE_GOLDEN=1`) to (re)write
 * the golden tree from the current output instead of asserting. Use this after an
 * intentional template/engine change, then review the resulting git diff.
 */
object GoldenSnapshot {

    private const val UPDATE_PROPERTY = "egs.golden.update"
    private const val UPDATE_ENV = "UPDATE_GOLDEN"
    private const val DIR_PROPERTY = "egs.golden.dir"
    private const val DEFAULT_DIR = "src/test/resources/golden"

    private val updateEnabled: Boolean
        get() = System.getProperty(UPDATE_PROPERTY)?.toBoolean() == true ||
            System.getenv(UPDATE_ENV) == "1"

    private val goldenRoot: File
        get() = File(System.getProperty(DIR_PROPERTY) ?: DEFAULT_DIR)

    /**
     * @param caseName stable directory name for this snapshot, e.g. `module-android`.
     * @param files generated `(relativePath, content)` pairs; null content is treated as empty.
     */
    fun verify(caseName: String, files: List<Pair<String, String?>>) {
        require(files.isNotEmpty()) { "Golden case '$caseName' produced no files" }
        val actual = files
            .associate { (path, content) -> path to normalize(content) }
            .toSortedMap()

        val caseDir = goldenRoot.resolve(caseName)

        if (updateEnabled) {
            writeGolden(caseDir, actual)
            return
        }

        check(caseDir.isDirectory) {
            "Golden case '$caseName' missing at ${caseDir.path}.\n" +
                "Create it with: ./gradlew :feature:scaffold:test -D$UPDATE_PROPERTY=true"
        }

        val expected = readGolden(caseDir)
        val problems = collectProblems(expected, actual)
        check(problems.isEmpty()) { failureMessage(caseName, problems) }
    }

    private fun writeGolden(caseDir: File, actual: Map<String, String>) {
        if (caseDir.exists()) caseDir.deleteRecursively()
        actual.forEach { (path, content) ->
            val target = caseDir.resolve(path)
            target.parentFile.mkdirs()
            target.writeText(content)
        }
    }

    private fun readGolden(caseDir: File): Map<String, String> {
        val basePath = caseDir.toPath().normalize()
        return caseDir.walkTopDown()
            .filter { it.isFile }
            .associate { file ->
                val relative = basePath.relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                relative to normalize(file.readText())
            }
            .toSortedMap()
    }

    private fun collectProblems(expected: Map<String, String>, actual: Map<String, String>): List<String> {
        val problems = mutableListOf<String>()
        (actual.keys - expected.keys).sorted().forEach {
            problems += "  [new] generated but absent from golden: $it"
        }
        (expected.keys - actual.keys).sorted().forEach {
            problems += "  [stale] in golden but no longer generated: $it"
        }
        expected.keys.intersect(actual.keys).sorted().forEach { path ->
            val expectedContent = expected.getValue(path)
            val actualContent = actual.getValue(path)
            if (expectedContent != actualContent) {
                problems += diff(path, expectedContent, actualContent)
            }
        }
        return problems
    }

    private fun normalize(content: String?): String = (content ?: "").replace("\r\n", "\n")

    /** Minimal common-prefix/suffix diff: shows only the changed region with a little context. */
    private fun diff(path: String, expected: String, actual: String): String {
        val expectedLines = expected.split("\n")
        val actualLines = actual.split("\n")
        var prefix = 0
        while (prefix < expectedLines.size && prefix < actualLines.size && expectedLines[prefix] == actualLines[prefix]) {
            prefix++
        }
        var suffixExpected = expectedLines.lastIndex
        var suffixActual = actualLines.lastIndex
        while (suffixExpected >= prefix && suffixActual >= prefix &&
            expectedLines[suffixExpected] == actualLines[suffixActual]
        ) {
            suffixExpected--
            suffixActual--
        }
        val context = 2
        return buildString {
            appendLine("  [diff] $path")
            val contextFrom = maxOf(0, prefix - context)
            for (i in contextFrom until prefix) appendLine("      ${expectedLines[i]}")
            for (i in prefix..suffixExpected) appendLine("    - ${expectedLines[i]}")
            for (i in prefix..suffixActual) appendLine("    + ${actualLines[i]}")
            val contextTo = minOf(expectedLines.lastIndex, suffixExpected + context)
            for (i in (suffixExpected + 1)..contextTo) appendLine("      ${expectedLines[i]}")
        }.trimEnd()
    }

    private fun failureMessage(caseName: String, problems: List<String>): String = buildString {
        appendLine("Golden mismatch for '$caseName' (${problems.size} problem(s)):")
        problems.forEach { appendLine(it) }
        appendLine()
        append("If this change is intended, refresh golden with: ./gradlew :feature:scaffold:test -D$UPDATE_PROPERTY=true")
    }
}
