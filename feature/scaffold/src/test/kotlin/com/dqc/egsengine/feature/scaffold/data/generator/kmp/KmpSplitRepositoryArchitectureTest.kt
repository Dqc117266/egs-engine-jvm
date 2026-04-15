package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.model.ColumnSchema
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class KmpSplitRepositoryArchitectureTest {

    private val template = ModuleTemplate(
        name = "todo",
        packageName = "org.example.feature.todo",
        conventionPluginId = null,
        layers = listOf("data", "domain", "presentation"),
        hasRes = false,
        namespace = null,
        projectType = "KMP",
        basePackage = "org.example",
        baseClassPackages = BaseClassPackages(
            resultClass = "template.core.base.network.domain.Result",
        ),
        apiResultClass = "template.core.base.network.NetworkResult",
        commonResultClass = "template.core.base.network.data.CommonResult",
        toResultPackage = "template.core.base.network.data",
    )

    private val userTable = TableSchema(
        tableName = "user",
        columns = listOf(
            ColumnSchema(
                name = "id",
                sqlType = "BIGINT",
                kotlinType = "Long",
                nullable = false,
                isPrimaryKey = true,
                isAutoIncrement = true,
                defaultValue = null,
                comment = null,
            ),
            ColumnSchema(
                name = "name",
                sqlType = "VARCHAR",
                kotlinType = "String",
                nullable = false,
                isPrimaryKey = false,
                isAutoIncrement = false,
                defaultValue = null,
                comment = null,
            ),
        ),
        primaryKey = "id",
    )

    @Test
    fun `combined repository extends only Db when Api absent on disk`() {
        val gen = KmpCombinedRepositoryGenerator()
        val combined = gen.generate(
            template = template,
            subProjectRoot = null,
            includeApi = false,
            includeDb = true,
        )!!
        assertTrue(combined.content!!.contains("interface TodoRepository : TodoDbRepository"))
    }

    @Test
    fun `combined repository extends Api and Db when both flags set`() {
        val gen = KmpCombinedRepositoryGenerator()
        val combined = gen.generate(
            template = template,
            subProjectRoot = null,
            includeApi = true,
            includeDb = true,
        )!!
        assertTrue(
            combined.content!!.contains(
                "interface TodoRepository : TodoApiRepository, TodoDbRepository",
            ),
        )
    }

    @Test
    fun `DB use case generator emits entity imports from database entity package`() {
        val engine = TemplateEngine(TemplateRegistry())
        val uc = KmpDatabaseUseCaseGenerator(engine)
        val files = uc.generate(template, listOf(userTable), projectRoot = null, subProjectRoot = null)
        val getAll = files.first { it.path.endsWith("GetUserAllUseCase.kt") }
        assertTrue(getAll.content!!.contains("org.example.feature.todo.generate.data.datasource.database.entity.UserEntity"))
    }

    @Test
    fun `DB use case merge preserves swagger domain module when api sync existed`() {
        val tmp = Files.createTempDirectory("egs-split-repo").toFile()
        try {
            val pkgPath = template.packageName.replace('.', '/')
            val diDir = tmp.resolve("feature/todo/src/commonMain/kotlin/$pkgPath/generate/di")
            diDir.mkdirs()
            val domain = diDir.resolve("GeneratedDomainModule.kt")
            domain.writeText(
                """
                package ${template.packageName}.generate.di

                import org.koin.core.module.dsl.singleOf
                import org.koin.dsl.module
                import ${template.packageName}.generate.domain.usecase.SampleUseCase

                internal val generatedDomainModule = module {
                    // egs-gen:swagger-usecases-begin
                    singleOf(::SampleUseCase)
                    // egs-gen:swagger-usecases-end

                    // egs-gen:db-usecases-begin

                    // egs-gen:db-usecases-end
                }
                """.trimIndent(),
            )

            val engine = TemplateEngine(TemplateRegistry())
            val uc = KmpDatabaseUseCaseGenerator(engine)
            val out = uc.generate(template, listOf(userTable), projectRoot = null, subProjectRoot = tmp)
            val dm = out.first { it.path.endsWith("GeneratedDomainModule.kt") }
            assertTrue(dm.content!!.contains("SampleUseCase"))
            assertTrue(dm.content!!.contains("GetUserAllUseCase"))
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun `repository impl generator emits delegation for api and db`() {
        val impl = KmpRepositoryImplGenerator().generateOrMerge(
            template = template,
            subProjectRoot = null,
            includeApi = true,
            includeDb = true,
        )!!
        assertTrue(impl.content!!.contains("TodoApiRepository by apiSupport"))
        assertTrue(impl.content!!.contains("TodoDbRepository by dbSupport"))
    }

    @Test
    fun `combined repository extends Prefs when includePrefs only`() {
        val gen = KmpCombinedRepositoryGenerator()
        val combined = gen.generate(
            template = template,
            subProjectRoot = null,
            includeApi = false,
            includeDb = false,
            includePrefs = true,
        )!!
        assertTrue(combined.content!!.contains("interface TodoRepository : TodoPrefsRepository"))
    }

    @Test
    fun `repository impl generator emits prefs delegation`() {
        val impl = KmpRepositoryImplGenerator().generateOrMerge(
            template = template,
            subProjectRoot = null,
            includeApi = false,
            includeDb = false,
            includePrefs = true,
        )!!
        assertTrue(impl.content!!.contains("TodoPrefsRepository by prefsSupport"))
    }
}
