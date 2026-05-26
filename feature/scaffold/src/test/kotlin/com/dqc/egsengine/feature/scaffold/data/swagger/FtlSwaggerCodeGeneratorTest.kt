package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FtlSwaggerCodeGeneratorTest {

    @Test
    fun `generate uses project local android swagger template overrides`() {
        val projectRoot = kotlin.io.path.createTempDirectory("swagger-ftl-override").toFile()
        val override = projectRoot.resolve(".egs/templates/android/swagger/UseCase.kt.ftl")
        override.parentFile.mkdirs()
        override.writeText(
            """
            package ${'$'}{packageName}

            internal class ${'$'}{useCaseName} {
                fun marker(): String = "project swagger override"
            }
            """.trimIndent(),
        )

        val files = FtlSwaggerCodeGenerator().generate(projectRoot, androidTemplate(), swaggerSpec())

        val useCase = files.single { it.path.endsWith("TopicUpdateTopicUseCase.kt") }
        assertTrue(useCase.content!!.contains("project swagger override"))
    }

    @Test
    fun `generate renders android repository impl with toData and toResult mapping`() {
        val projectRoot = kotlin.io.path.createTempDirectory("swagger-ftl-android").toFile()

        val files = FtlSwaggerCodeGenerator().generate(projectRoot, androidTemplate(), swaggerSpec())

        val repository = files.single { it.path.endsWith("domain/repository/TaskRepository.kt") }
        assertTrue(repository.content!!.contains("suspend fun topicUpdateTopic("))
        assertTrue(repository.content!!.contains("body: TopicSaveReqVO"))
        assertTrue(repository.content!!.contains("): Result<Boolean>"))

        val repositoryImpl = files.single { it.path.endsWith("data/repository/TaskRepositoryImpl.kt") }
        assertTrue(repositoryImpl.content!!.contains("service.topicUpdateTopic(body.toData()).toResult()"))
    }

    @Test
    fun `generate renders kmp swagger templates into commonMain with ktorfit imports`() {
        val projectRoot = kotlin.io.path.createTempDirectory("swagger-ftl-kmp").toFile()

        val files = FtlSwaggerCodeGenerator().generate(projectRoot, kmpTemplate(), swaggerSpec())

        val service = files.single { it.path.endsWith("TaskApiService.kt") }
        assertTrue(
            service.path.endsWith(
                "feature/task/src/commonMain/kotlin/org/mifos/feature/task/data/datasource/api/service/TaskApiService.kt",
            ),
        )
        assertTrue(service.content!!.contains("import de.jensklingenberg.ktorfit.http.PUT"))
        assertTrue(service.content!!.contains("import de.jensklingenberg.ktorfit.http.Body"))
        assertTrue(files.any { it.path.endsWith("generate/di/GeneratedDataModule.kt") })
        assertTrue(files.none { it.path.contains("/src/main/kotlin/") })
    }

    private fun androidTemplate(): ModuleTemplate =
        ModuleTemplate(
            name = "task",
            packageName = "com.dqc.example.feature.task",
            conventionPluginId = "com.dqc.example.convention.feature",
            layers = listOf("data", "domain", "presentation"),
            hasRes = true,
            namespace = "com.dqc.example.feature.task",
            projectType = "ANDROID",
            basePackage = "com.dqc.example",
            baseClassPackages = BaseClassPackages(
                resultClass = "com.dqc.example.feature.base.domain.result.Result",
                retrofitProvider = "com.dqc.example.feature.common.network.DynamicRetrofitProvider",
            ),
            apiResultClass = "com.dqc.example.feature.base.data.retrofit.ApiResult",
            commonResultClass = "com.dqc.example.feature.base.data.retrofit.CommonResult",
            toResultPackage = "com.dqc.example.feature.base.data.retrofit",
        )

    private fun kmpTemplate(): ModuleTemplate =
        ModuleTemplate(
            name = "task",
            packageName = "org.mifos.feature.task",
            conventionPluginId = null,
            layers = listOf("data", "domain", "presentation"),
            hasRes = false,
            namespace = null,
            projectType = "KMP",
            basePackage = "org.mifos",
            baseClassPackages = BaseClassPackages(
                resultClass = "template.core.base.network.domain.Result",
            ),
            apiResultClass = "template.core.base.network.data.ApiResult",
            commonResultClass = "template.core.base.network.data.CommonResult",
            toResultPackage = "template.core.base.network.data",
        )

    private fun swaggerSpec(): SwaggerSpec =
        SwaggerSpec(
            schemas = listOf(
                SwaggerSchema(
                    name = "TopicSaveReqVO",
                    properties = listOf(
                        SwaggerProperty(
                            name = "id",
                            originalName = "id",
                            type = SwaggerType.Primitive(PrimitiveKind.LONG),
                            required = true,
                        ),
                        SwaggerProperty(
                            name = "name",
                            originalName = "name",
                            type = SwaggerType.Primitive(PrimitiveKind.STRING),
                            required = true,
                        ),
                    ),
                ),
                SwaggerSchema(
                    name = "CommonResultBoolean",
                    properties = listOf(
                        SwaggerProperty("code", "code", SwaggerType.Primitive(PrimitiveKind.INT), false),
                        SwaggerProperty("msg", "msg", SwaggerType.Primitive(PrimitiveKind.STRING), false),
                        SwaggerProperty("data", "data", SwaggerType.Primitive(PrimitiveKind.BOOLEAN), false),
                    ),
                ),
            ),
            operations = listOf(
                SwaggerOperation(
                    operationId = "topicUpdateTopic",
                    method = "put",
                    path = "/admin-api/ai/topic/update",
                    params = emptyList(),
                    requestBody = SwaggerType.ModelRef("TopicSaveReqVO"),
                    responseBody = SwaggerType.ModelRef("CommonResultBoolean"),
                ),
            ),
        )
}
