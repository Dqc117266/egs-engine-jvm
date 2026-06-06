package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class KmpDatabaseGeneratedDataModuleUpdaterTest {

    @Test
    fun `fills database markers in existing GeneratedDataModule`() {
        val root = kotlin.io.path.createTempDirectory("kmp-gdm").toFile()
        val moduleName = "storage"
        val pkg = "org.example.feature.storage"
        val pkgPath = pkg.replace('.', '/')
        val file = root.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/di/GeneratedDataModule.kt",
        )
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.example.feature.storage.generate.di

            import de.jensklingenberg.ktorfit.Ktorfit
            import org.koin.dsl.module

            internal val generatedDataModule = module {
                single { get<Ktorfit>().createX() }

                // egs-gen:database-begin

                // egs-gen:database-end
            }
            """.trimIndent() + "\n",
        )

        val sql = """
            CREATE TABLE t1 (
                id BIGINT NOT NULL AUTO_INCREMENT,
                PRIMARY KEY (id)
            );
        """.trimIndent()
        val tables = DdlParser().parse(sql)
        val template = ModuleTemplate(
            name = moduleName,
            packageName = pkg,
            conventionPluginId = null,
            layers = listOf("data"),
            hasRes = false,
            namespace = null,
            projectType = "KMP",
            basePackage = "org.example",
            baseClassPackages = BaseClassPackages(),
        )

        KmpDatabaseGeneratedDataModuleUpdater().apply(
            root,
            moduleName,
            template,
            tables,
            includeDbRepositorySupport = true,
        )

        val text = file.readText()
        assertTrue(text.contains("single<StorageDatabase>"))
        assertTrue(text.contains("singleOf(::StorageDatabaseDataSource)"))
        assertTrue(text.contains("singleOf(::GeneratedStorageDbRepositorySupport)"))
        assertTrue(text.contains("get<StorageDatabase>().t1Dao()"))
        assertTrue(text.contains("org.example.core.base.database.AppRoomDatabase"))
    }

    @Test
    fun `omits DbRepositorySupport binding when includeDbRepositorySupport is false`() {
        val root = kotlin.io.path.createTempDirectory("kmp-gdm-no-repo").toFile()
        val moduleName = "storage"
        val pkg = "org.example.feature.storage"
        val pkgPath = pkg.replace('.', '/')
        val file = root.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/di/GeneratedDataModule.kt",
        )
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.example.feature.storage.generate.di

            import de.jensklingenberg.ktorfit.Ktorfit
            import org.koin.dsl.module

            internal val generatedDataModule = module {
                single { get<Ktorfit>().createX() }

                // egs-gen:database-begin

                // egs-gen:database-end
            }
            """.trimIndent() + "\n",
        )

        val sql = """
            CREATE TABLE t1 (
                id BIGINT NOT NULL AUTO_INCREMENT,
                PRIMARY KEY (id)
            );
        """.trimIndent()
        val tables = DdlParser().parse(sql)
        val template = ModuleTemplate(
            name = moduleName,
            packageName = pkg,
            conventionPluginId = null,
            layers = listOf("data"),
            hasRes = false,
            namespace = null,
            projectType = "KMP",
            basePackage = "org.example",
            baseClassPackages = BaseClassPackages(),
        )

        KmpDatabaseGeneratedDataModuleUpdater().apply(
            root,
            moduleName,
            template,
            tables,
            includeDbRepositorySupport = false,
        )

        val text = file.readText()
        assertTrue(text.contains("single<StorageDatabase>"))
        assertTrue(text.contains("singleOf(::StorageDatabaseDataSource)"))
        assertTrue(!text.contains("GeneratedStorageDbRepositorySupport"))
    }
}
