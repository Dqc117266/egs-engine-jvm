package com.dqc.egsengine.feature.scaffold.data.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KotlinMemberInspectorTest {

    private val sampleVm =
        """
        package p

        internal class FooViewModel(
            private val alpha: AlphaUseCase,
            private val beta: BetaUseCase,
        ) : BaseViewModel<FooContract.State, FooContract.Intent, FooContract.Effect>(
            initialState = FooContract.State(),
        ) {

            override fun registerIntents() {
                registerIntent<FooContract.Intent.Alpha> {
                    handleAlpha()
                }
            }

            private fun handleAlpha() { }
        }
        """.trimIndent()

    @Test
    fun `primary constructor params`() {
        val ps = KotlinMemberInspector.parsePrimaryConstructorParams(sampleVm, "FooViewModel")
        assertEquals(2, ps.size)
        assertEquals("alpha", ps[0].name)
        assertEquals("AlphaUseCase", ps[0].type)
        assertEquals("beta", ps[1].name)
        assertEquals("BetaUseCase", ps[1].type)
    }

    @Test
    fun `sealed Intent members`() {
        val src =
            """
            sealed class Intent : UiIntent {
                data object Load : Intent()
                data class Submit(val id: Long) : Intent()
            }
            """.trimIndent()
        val names = KotlinMemberInspector.sealedIntentMemberNames(src)
        assertEquals(setOf("Load", "Submit"), names)
    }

    @Test
    fun `State properties`() {
        val src =
            """
            data class State(
                val isLoading: Boolean = false,
                val error: String? = null,
                val items: List<String> = emptyList(),
            ) : UiState
            """.trimIndent()
        val props = KotlinMemberInspector.dataClassPropertyNames(src)
        assertEquals(setOf("isLoading", "error", "items"), props)
    }

    @Test
    fun `registerIntent branch names`() {
        val branches = KotlinMemberInspector.registerIntentBranchNames(sampleVm, "Foo")
        assertEquals(setOf("Alpha"), branches)
    }

    @Test
    fun `handler names`() {
        val h = KotlinMemberInspector.privateHandlerFunctionNames(sampleVm)
        assertTrue(h.contains("handleAlpha"))
    }

    @Test
    fun `importsBlock endExclusive is CRLF safe and points after last import`() {
        val lf =
            """
            package p

            import com.example.A
            import com.example.BaseViewModel

            class T
            """.trimIndent()
        val crlf = lf.replace("\n", "\r\n")
        val blockLf = KotlinMemberInspector.importsBlock(lf)!!
        val blockCrlf = KotlinMemberInspector.importsBlock(crlf)!!
        val tailLf = lf.substring(blockLf.endExclusive)
        val tailCrlf = crlf.substring(blockCrlf.endExclusive)
        // After the last import line's newline, the next segment is the blank line then `class T`.
        assertTrue(tailLf.startsWith("\nclass T"), "LF: remainder should be after blank line following imports")
        assertTrue(
            tailCrlf.startsWith("\r\nclass T"),
            "CRLF: remainder should match LF structure; bad endExclusive would split an import mid-line",
        )
        assertTrue(
            lf.substring(blockLf.startIndex).startsWith("import "),
            "startIndex should be first import line",
        )
    }
}
