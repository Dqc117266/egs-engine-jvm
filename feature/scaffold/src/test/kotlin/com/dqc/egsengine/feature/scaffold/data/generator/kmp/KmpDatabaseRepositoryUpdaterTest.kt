package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KmpDatabaseRepositoryUpdaterTest {

    @Test
    fun `patches GeneratedRepositorySupport and RepositoryImpl with dbDataSource`(@TempDir temp: File) {
        val moduleName = "todo"
        val pkg = "com.example.feature.todo"
        val pkgPath = pkg.replace('.', '/')
        val root = temp.resolve("client").apply { mkdirs() }
        val supportFile = root.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/data/repository/GeneratedTodoRepositorySupport.kt",
        )
        supportFile.parentFile.mkdirs()
        supportFile.writeText(
            """
            package $pkg.generate.data.repository

            import $pkg.generate.data.datasource.api.service.TodoKtorfitService
            import $pkg.generate.domain.repository.TodoRepository

            internal open class GeneratedTodoRepositorySupport(
                private val service: TodoKtorfitService,
            ) : TodoRepository {
                override suspend fun sample(): String = "x"
            }
            """.trimIndent() + "\n",
        )

        val implFile = root.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/data/repository/TodoRepositoryImpl.kt",
        )
        implFile.parentFile.mkdirs()
        implFile.writeText(
            """
            package $pkg.data.repository

            import $pkg.generate.data.datasource.api.service.TodoKtorfitService
            import $pkg.generate.data.repository.GeneratedTodoRepositorySupport

            internal class TodoRepositoryImpl(
                service: TodoKtorfitService,
            ) : GeneratedTodoRepositorySupport(service) {
            }
            """.trimIndent() + "\n",
        )

        val template = ModuleTemplate(
            name = moduleName,
            packageName = pkg,
            conventionPluginId = null,
            layers = listOf("data", "domain", "presentation"),
            hasRes = false,
            namespace = null,
            projectType = "KMP",
            basePackage = "com.example",
            baseClassPackages = BaseClassPackages(),
        )

        KmpDatabaseRepositoryUpdater().apply(root, moduleName, template)

        val supportOut = supportFile.readText()
        assertTrue(supportOut.contains("protected val dbDataSource: TodoDatabaseDataSource"))
        assertTrue(supportOut.contains("import $pkg.generate.data.datasource.database.TodoDatabaseDataSource"))

        val implOut = implFile.readText()
        assertTrue(implOut.contains("dbDataSource: TodoDatabaseDataSource"))
        assertTrue(implOut.contains("GeneratedTodoRepositorySupport(service, dbDataSource)"))
    }
}
