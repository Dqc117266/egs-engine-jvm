package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageTemplateModelMapperTest {

    @Test
    fun `Contract state uses short type name and import for domain VO`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<PageResultAppAiChatSessionRespVO>",
            parameters = listOf(
                UseCaseParam("topicId", "kotlin.Long?"),
                UseCaseParam("pageNo", "kotlin.Int"),
                UseCaseParam("pageSize", "kotlin.Int"),
            ),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toPageTemplateModel()
        val f = m.stateFields.single()
        assertEquals("appAiChatGetSessionPage", f.name)
        assertEquals(
            "com.dqc.androidtest.feature.todo.generate.domain.model.PageResultAppAiChatSessionRespVO",
            f.typeFqn,
        )
        assertEquals("PageResultAppAiChatSessionRespVO", f.typeContractRef)
        assertTrue(
            m.contractImports.any {
                it == "import com.dqc.androidtest.feature.todo.generate.domain.model.PageResultAppAiChatSessionRespVO"
            },
        )
        val intent = m.intentInners.single()
        val params = intent.params
        assertEquals("Long?", params[0].kotlinType)
        assertEquals("Int", params[1].kotlinType)
        assertEquals("Int", params[2].kotlinType)
    }

    @Test
    fun `legacy domain model FQN in use case is rewritten to generate domain model`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<com.dqc.androidtest.feature.todo.domain.model.PageResultAppAiChatSessionRespVO>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        )
        val f = pt.toPageTemplateModel().stateFields.single()
        assertEquals(
            "com.dqc.androidtest.feature.todo.generate.domain.model.PageResultAppAiChatSessionRespVO",
            f.typeFqn,
        )
    }
}
