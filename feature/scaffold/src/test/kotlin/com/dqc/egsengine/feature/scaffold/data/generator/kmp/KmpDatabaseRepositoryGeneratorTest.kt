package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.swagger.PrimitiveKind
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerProperty
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
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

    @Test
    fun `with Swagger spec maps user table to UserRespVO in repository API`() {
        val url = javaClass.classLoader.getResource("ddl/user.sql")
            ?: error("ddl/user.sql not on test classpath")
        val tables = DdlParser().parseFile(java.io.File(url.toURI()))

        val userVo = SwaggerSchema(
            name = "UserRespVO",
            properties = listOf(
                SwaggerProperty("id", "id", SwaggerType.Primitive(PrimitiveKind.LONG), true),
                SwaggerProperty("username", "username", SwaggerType.Primitive(PrimitiveKind.STRING), true),
                SwaggerProperty("email", "email", SwaggerType.Primitive(PrimitiveKind.STRING), true),
                SwaggerProperty("avatarUrl", "avatarUrl", SwaggerType.Primitive(PrimitiveKind.STRING), false),
                SwaggerProperty("createdAt", "createdAt", SwaggerType.Primitive(PrimitiveKind.LONG), true),
            ),
        )
        val spec = SwaggerSpec(schemas = listOf(userVo), operations = emptyList())

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

        val files = gen.generateDbOnlyRepository(template, tables, projectRoot = null, spec = spec)
        val repo = files.first { it.path.endsWith("/StorageDbRepository.kt") }.content!!
        assertTrue(repo.contains("suspend fun getUserAll(): List<UserRespVO>"))
        assertTrue(repo.contains("import org.example.feature.storage.generate.domain.model.UserRespVO"))

        val support = files.first { it.path.endsWith("/GeneratedStorageDbRepositorySupport.kt") }.content!!
        assertTrue(support.contains("import org.example.feature.storage.generate.data.datasource.database.mapper.*"))
        assertTrue(support.contains(".map { it.toDomain() }"))
        assertTrue(support.contains("UserSessionEntity"))

        // user_session has no matching Swagger schema: keep Entity in interface
        assertTrue(repo.contains("suspend fun getUserSessionAll(): List<UserSessionEntity>"))
    }
}
