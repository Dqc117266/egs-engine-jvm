package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpDatabaseCodeGeneratorTest {

    @Test
    fun `generates commonMain paths under generate data datasource database`() {
        val url = javaClass.classLoader.getResource("ddl/user.sql")
            ?: error("ddl/user.sql not on test classpath")
        val tables = DdlParser().parseFile(java.io.File(url.toURI()))

        val engine = TemplateEngine(TemplateRegistry())
        val gen = KmpDatabaseCodeGenerator(engine)

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

        val files = gen.generate(template, tables, projectRoot = null)
        val paths = files.map { it.path }.toSet()

        assertTrue(paths.any { it.endsWith("/entity/UserEntity.kt") })
        assertTrue(paths.any { it.endsWith("/entity/UserSessionEntity.kt") })
        assertTrue(paths.any { it.endsWith("/dao/UserDao.kt") })
        assertTrue(paths.any { it.endsWith("/dao/UserSessionDao.kt") })
        assertTrue(paths.any { it.endsWith("/StorageDatabase.kt") })
        assertTrue(paths.any { it.endsWith("/StorageDatabaseDataSource.kt") })
        assertTrue(
            paths.all {
                it.startsWith("feature/storage/src/commonMain/kotlin/org/example/feature/storage/generate/data/datasource/database")
            },
        )

        val userEntity = files.first { it.path.endsWith("/entity/UserEntity.kt") }.content!!
        assertFalse("""\)\s*\n\s*,""".toRegex().containsMatchIn(userEntity), "entity ctor: comma should not be on its own line after ')'")
        assertTrue(userEntity.contains("val id: Long,"), "comma stays on same line as property when more columns follow")
    }
}
