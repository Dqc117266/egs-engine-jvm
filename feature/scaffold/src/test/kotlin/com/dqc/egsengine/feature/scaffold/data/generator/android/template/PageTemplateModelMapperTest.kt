package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `use case handlers mark resultBased for API Result return type`() {
        val uc = UseCaseInfo(
            name = "FooUseCase",
            packageName = "p",
            path = "x",
            returnType = "template.core.base.network.domain.Result<Boolean>",
            parameters = emptyList(),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        val h = m.useCaseHandlers.single()
        assertTrue(h.resultBased)
        assertFalse(h.flowBased)
        assertTrue(m.hasResultBasedHandler)
    }

    @Test
    fun `use case handlers mark flowBased for Flow return type`() {
        val uc = UseCaseInfo(
            name = "ObserveUserIdUseCase",
            packageName = "p",
            path = "x",
            returnType = "kotlinx.coroutines.flow.Flow<String>",
            parameters = emptyList(),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        val h = m.useCaseHandlers.single()
        assertFalse(h.resultBased)
        assertTrue(h.flowBased)
        assertFalse(m.hasResultBasedHandler)
        assertTrue(m.stateFields.isEmpty())
    }

    @Test
    fun `Room Entity params resolve to database entity package with short type and import`() {
        val uc = UseCaseInfo(
            name = "UpdateUserSessionUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entity", "UserSessionEntity")),
        )
        val m = PageTemplate(
            pageName = "Todolists",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        val p = m.intentInners.single().params.single()
        assertEquals(
            "com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserSessionEntity",
            p.kotlinType,
        )
        assertEquals("UserSessionEntity", p.kotlinTypeContractRef)
        assertTrue(
            m.contractImports.any {
                it == "import com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserSessionEntity"
            },
        )
    }

    @Test
    fun `plain Unit return does not add state field and uses non-result handler`() {
        val unitUc = UseCaseInfo(
            name = "UpdateUserSessionUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entity", "UserSessionEntity")),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(unitUc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        assertTrue(m.stateFields.isEmpty())
        val h = m.useCaseHandlers.single()
        assertFalse(h.resultBased)
        assertFalse(h.flowBased)
        assertFalse(m.hasResultBasedHandler)
    }

    @Test
    fun `Result Unit return has no state field but still uses result handler branch`() {
        val uc = UseCaseInfo(
            name = "DeleteThingUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<Unit>",
            parameters = emptyList(),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        assertTrue(m.stateFields.isEmpty())
        val h = m.useCaseHandlers.single()
        assertTrue(h.resultBased)
        assertFalse(h.flowBased)
        assertTrue(m.hasResultBasedHandler)
    }
}
