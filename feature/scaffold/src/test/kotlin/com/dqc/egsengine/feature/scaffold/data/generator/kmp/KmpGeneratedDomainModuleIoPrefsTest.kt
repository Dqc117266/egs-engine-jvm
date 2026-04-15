package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpGeneratedDomainModuleIoPrefsTest {

    @Test
    fun `merge preserves prefs block when swagger template is regenerated`() {
        val existing = """
            package p.generate.di

            import org.koin.dsl.module

            internal val generatedDataModule = module {
                single { "api" }
                // egs-gen:database-begin
                single { "db" }
                // egs-gen:database-end

                // egs-gen:prefs-begin
                singleOf(::PrefsDataSource)
                // egs-gen:prefs-end
            }
        """.trimIndent()

        val generated = """
            package p.generate.di

            import org.koin.dsl.module

            internal val generatedDataModule = module {
                single { "freshApi" }
                // egs-gen:database-begin

                // egs-gen:database-end

                // egs-gen:prefs-begin

                // egs-gen:prefs-end
            }
        """.trimIndent()

        val merged = KmpGeneratedDomainModuleIo.mergeGeneratedDataModulePreservingDatabaseBlock(existing, generated)
        assertTrue(merged.contains("single { \"freshApi\" }"))
        assertTrue(merged.contains("single { \"db\" }"))
        assertTrue(merged.contains("singleOf(::PrefsDataSource)"))
    }
}
