package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpGeneratedDomainModuleIoPrefsTest {
    @Test
    fun `replacePrefsUseCasesBlock fills empty consecutive markers from swagger template`() {
        val before =
            """
            internal val generatedDomainModule = module {
                // egs-gen:swagger-usecases-begin
                singleOf(::FooUseCase)
                // egs-gen:swagger-usecases-end

                // egs-gen:db-usecases-end

                // egs-gen:prefs-usecases-begin
                // egs-gen:prefs-usecases-end
            }
            """.trimIndent()

        val after =
            KmpGeneratedDomainModuleIo.replacePrefsUseCasesBlock(
                before,
                listOf("GetUserIdUseCase", "SetUserIdUseCase", "ObserveUserIdUseCase"),
            )
        assertTrue(after.contains("singleOf(::GetUserIdUseCase)"))
        assertTrue(after.contains("singleOf(::SetUserIdUseCase)"))
        assertTrue(after.contains("singleOf(::ObserveUserIdUseCase)"))
        assertTrue(!after.contains("prefs-usecases-begin\n                // egs-gen:prefs-usecases-end"))
    }

    @Test
    fun `merge preserves prefs block when swagger template is regenerated`() {
        val existing =
            """
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

        val generated =
            """
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

    @Test
    fun `merge preserves imports and indentation for database blocks`() {
        val existing =
            """
            package p.generate.di

            import org.koin.dsl.module
            import androidx.room.RoomDatabase
            import p.data.TodoDatabase

            internal val generatedDataModule = module {
                single { "api" }
                // egs-gen:database-begin
                single<TodoDatabase> { get() }
                // egs-gen:database-end
            }
            """.trimIndent()

        val generated =
            """
            package p.generate.di

            import org.koin.dsl.module

            internal val generatedDataModule = module {
                single { "freshApi" }
                // egs-gen:database-begin

                // egs-gen:database-end
            }
            """.trimIndent()

        val merged = KmpGeneratedDomainModuleIo.mergeGeneratedDataModulePreservingDatabaseBlock(existing, generated)
        assertTrue(merged.contains("import androidx.room.RoomDatabase"))
        assertTrue(merged.contains("import p.data.TodoDatabase"))
        assertTrue(merged.contains("single<TodoDatabase>"))
        assertFalse(merged.contains("\n        // egs-gen:database-begin"))
        assertFalse(merged.contains("\nsingle<TodoDatabase>"))
    }
}
