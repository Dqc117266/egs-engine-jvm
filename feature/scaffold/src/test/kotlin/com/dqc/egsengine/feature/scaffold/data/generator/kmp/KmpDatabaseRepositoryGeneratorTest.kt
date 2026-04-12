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
    fun `generates DB-only repository slice interface and support`() {
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

        assertTrue(paths.any { it.endsWith("/StorageDbRepository.kt") })
        assertTrue(paths.any { it.endsWith("/GeneratedStorageDbRepositorySupport.kt") })

        val repo = files.first { it.path.endsWith("/StorageDbRepository.kt") }.content!!
        assertTrue(repo.contains("interface StorageDbRepository"))
        assertTrue(repo.contains("suspend fun getUserAll()"))

        val support = files.first { it.path.endsWith("/GeneratedStorageDbRepositorySupport.kt") }.content!!
        assertTrue(support.contains("class GeneratedStorageDbRepositorySupport"))
        assertTrue(support.contains("dbDataSource.getUserAll()"))
    }
}
