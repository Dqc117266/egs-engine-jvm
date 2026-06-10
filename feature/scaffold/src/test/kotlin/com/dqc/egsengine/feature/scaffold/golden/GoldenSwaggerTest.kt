package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.scaffold.data.swagger.FtlSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.PrimitiveKind
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerOperation
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerProperty
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

/**
 * Full-tree golden snapshot of `create api` (swagger) output for ANDROID and KMP projects.
 * Catches any unintended change in the wired `android/swagger` and `kmp/swagger` FTL templates.
 */
class GoldenSwaggerTest {
    @Test
    fun `android swagger golden`() {
        val files = FtlSwaggerCodeGenerator().generate(tempRoot(), androidTemplate(), swaggerSpec())
        GoldenSnapshot.verify("api-android", files.map { it.path to it.content })
    }

    @Test
    fun `kmp swagger golden`() {
        val files = FtlSwaggerCodeGenerator().generate(tempRoot(), kmpTemplate(), swaggerSpec())
        GoldenSnapshot.verify("api-kmp", files.map { it.path to it.content })
    }

    private fun tempRoot() = createTempDirectory("golden-swagger").toFile()

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
            baseClassPackages =
                BaseClassPackages(
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
            baseClassPackages =
                BaseClassPackages(
                    resultClass = "template.core.base.network.domain.Result",
                ),
            apiResultClass = "template.core.base.network.data.ApiResult",
            commonResultClass = "template.core.base.network.data.CommonResult",
            toResultPackage = "template.core.base.network.data",
        )

    private fun swaggerSpec(): SwaggerSpec =
        SwaggerSpec(
            schemas =
                listOf(
                    SwaggerSchema(
                        name = "TopicSaveReqVO",
                        properties =
                            listOf(
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
                        properties =
                            listOf(
                                SwaggerProperty("code", "code", SwaggerType.Primitive(PrimitiveKind.INT), false),
                                SwaggerProperty("msg", "msg", SwaggerType.Primitive(PrimitiveKind.STRING), false),
                                SwaggerProperty("data", "data", SwaggerType.Primitive(PrimitiveKind.BOOLEAN), false),
                            ),
                    ),
                ),
            operations =
                listOf(
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
