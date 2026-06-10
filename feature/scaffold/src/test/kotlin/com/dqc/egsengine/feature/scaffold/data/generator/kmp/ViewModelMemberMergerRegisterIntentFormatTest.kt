/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.common.ViewModelMergeSnippet
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for [ViewModelMemberMerger.insertIntoRegisterIntents]:
 * - Newly appended `registerIntent<¡­> { ¡­ }` blocks must not contain blank lines.
 * - Trailing whitespace left by prior merges is stripped so blank lines do not accumulate.
 */
class ViewModelMemberMergerRegisterIntentFormatTest {
    private val useCase =
        UseCaseInfo(
            name = "FooUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = emptyList(),
        )

    private fun snippet(
        block: String,
        importLine: String = "import x.FooUseCase",
    ): ViewModelMergeSnippet = ViewModelMergeSnippet(
        useCase = useCase,
        intentMemberText = "",
        stateFieldText = null,
        viewModelImportLines = listOf(importLine),
        ctorParamLine = "    private val foo: FooUseCase,\n",
        registerIntentBlock = block,
        handlerFunction = null,
    )

    @Test
    fun `block with stray blank lines inside is compacted on insert`() {
        val vm =
            """
            package x

            internal class PViewModel {
                override fun registerIntents() {
                    registerIntent<PContract.Intent.Old> {
                        handleOld()
                    }
                }
            }
            """.trimIndent()

        val messyBlock =
            """
            registerIntent<PContract.Intent.Foo> {

                handleFoo()

            }
            """.trimIndent()

        val result =
            ViewModelMemberMerger.mergeViewModel(
                vmText = vm,
                pascalName = "P",
                snippets = listOf(snippet(messyBlock)),
                existingCtorTypes = emptySet(),
                existingRegisterBranches = emptySet(),
                existingHandlerNames = emptySet(),
            )
        val text = result.text

        val block =
            Regex(
                """registerIntent<PContract\.Intent\.Foo> \{[\s\S]*?\n\s*\}""",
            ).find(text)?.value ?: error("new register block not found:\n$text")
        assertFalse(
            block.lines().any { it.isBlank() },
            "expected compact block without blank lines, got:\n$block",
        )
        assertTrue(
            text.contains("        registerIntent<PContract.Intent.Foo> {\n            handleFoo()\n        }"),
            "expected normalised 8/12 indentation, got:\n$text",
        )
    }

    @Test
    fun `repeated merges do not accumulate blank lines before closing brace`() {
        val vm =
            """
            package x

            internal class PViewModel {
                override fun registerIntents() {
                    registerIntent<PContract.Intent.Old> {
                        handleOld()
                    }
                }
            }
            """.trimIndent()

        val block =
            """
            registerIntent<PContract.Intent.Foo> {
                handleFoo()
            }
            """.trimIndent()

        var text =
            ViewModelMemberMerger
                .mergeViewModel(
                    vmText = vm,
                    pascalName = "P",
                    snippets = listOf(snippet(block)),
                    existingCtorTypes = emptySet(),
                    existingRegisterBranches = emptySet(),
                    existingHandlerNames = emptySet(),
                ).text

        val snd =
            snippet(
                """
                registerIntent<PContract.Intent.Bar> {
                    handleBar()
                }
                """.trimIndent(),
            ).copy(
                useCase = useCase.copy(name = "BarUseCase"),
            )
        text =
            ViewModelMemberMerger
                .mergeViewModel(
                    vmText = text,
                    pascalName = "P",
                    snippets = listOf(snd),
                    existingCtorTypes = setOf("FooUseCase"),
                    existingRegisterBranches = setOf("PContract.Intent.Foo"),
                    existingHandlerNames = emptySet(),
                ).text

        // There should be at most one blank line between sibling blocks and no blank line before the
        // closing brace of `registerIntents`.
        val registerBody =
            Regex("""registerIntents\s*\(\s*\)\s*\{([\s\S]*?)\n\s*\}\s*\n""")
                .find(text)
                ?.groupValues
                ?.get(1)
                ?: error("registerIntents body not found in:\n$text")
        val trailing = registerBody.takeLastWhile { it == '\n' || it == ' ' || it == '\t' || it == '\r' }
        assertFalse(
            trailing.count { it == '\n' } > 1,
            "expected no blank line before closing `}`, got ${trailing.length} trailing whitespace char(s):\n---\n$registerBody\n---\nfull:\n$text",
        )
        assertFalse(
            registerBody.contains("\n\n\n"),
            "expected no double-blank between sibling blocks, got:\n$registerBody",
        )
    }
}
