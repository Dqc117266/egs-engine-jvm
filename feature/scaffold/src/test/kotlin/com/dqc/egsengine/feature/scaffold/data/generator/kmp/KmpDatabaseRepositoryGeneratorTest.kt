package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpDatabaseRepositoryGeneratorTest {

    @Test
    fun `generates DB-only repository interface support and impl`() {
        val url = javaClass.classLoader.getResource("ddl/user.sql")
            ?: error("ddl/user.sql not on test classpath")
        val tables = DdlParser().parseFile(java.io.File(url.toURI()))

        val engine = TemplateEngine(TemplateRegistry())
        val gen = KmpDatabaseRepositoryGenerator(engine)

        val template = ModuleTemplate(
            name = "storage",
            packageName = "org.example.feature.storage",
            conventionPluginId = null,
            layers = listOf("data", "domain", "presentation"),
            hasRes = false,
            namespace = null,
            projectType = "KMP",
            basePackage = "org.example",
            baseClassPackages = BaseClassPackages(
                resultClass = "org.example.feature.base.domain.result.Result",
            ),
        )

        val files = gen.generateDbOnlyRepository(template, tables, projectRoot = null)
        val paths = files.map { it.path }

        assertTrue(paths.any { it.endsWith("/StorageRepository.kt") })
        assertTrue(paths.any { it.endsWith("/GeneratedStorageRepositorySupport.kt") })
        assertTrue(paths.any { it.endsWith("/StorageRepositoryImpl.kt") })

        val repo = files.first { it.path.endsWith("/StorageRepository.kt") }.content!!
        assertTrue(repo.contains("interface StorageRepository"))
        assertTrue(repo.contains("suspend fun getUserAll()"))

        val support = files.first { it.path.endsWith("/GeneratedStorageRepositorySupport.kt") }.content!!
        assertTrue(support.contains("class GeneratedStorageRepositorySupport"))
        assertTrue(support.contains("dbDataSource.getUserAll()"))

        val impl = files.first { it.path.endsWith("/StorageRepositoryImpl.kt") }.content!!
        assertTrue(impl.contains("class StorageRepositoryImpl"))
        assertTrue(impl.contains("StorageDatabaseDataSource"))
    }
}
