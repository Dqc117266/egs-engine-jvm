package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpSwaggerCodeGeneratorTest {

    @Test
    fun `kmp swagger codegen uses commonMain and generate package paths`() {
        val engine = TemplateEngine(TemplateRegistry())
        val renderer = KmpSwaggerTemplateRenderer(engine)
        val gen = KmpSwaggerCodeGenerator(
            renderer,
            KmpCombinedRepositoryGenerator(),
            KmpRepositoryImplGenerator(),
        )

        val template = ModuleTemplate(
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
                pageResultClass = "template.core.base.network.pagination.PageResult",
            ),
            apiResultClass = "template.core.base.network.NetworkResult",
            commonResultClass = "template.core.base.network.data.CommonResult",
            toResultPackage = "template.core.base.network.data",
        )

        val spec = SwaggerSpec(
            schemas = emptyList(),
            operations = emptyList(),
        )

        val files = gen.generate(template, spec)
        val paths = files.map { it.path }.toSet()

        assertTrue(
            paths.any {
                it == "feature/todo/src/commonMain/kotlin/org/example/feature/todo/generate/di/GeneratedDataModule.kt"
            },
        )
        assertTrue(
            paths.any {
                it == "feature/todo/src/commonMain/kotlin/org/example/feature/todo/generate/di/GeneratedDomainModule.kt"
            },
        )
        assertTrue(
            paths.any {
                it.contains("generate/data/datasource/api/service/TodoKtorfitService.kt")
            },
        )
        assertTrue(
            paths.any {
                it.contains("generate/domain/repository/TodoApiRepository.kt")
            },
        )
        assertTrue(
            paths.any {
                it.contains("generate/domain/repository/TodoRepository.kt")
            },
        )
        assertTrue(
            paths.any {
                it.contains("data/repository/TodoRepositoryImpl.kt")
            },
        )
    }
}
