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
    fun `concrete PageResult use case becomes offset paging with PagingListState fields and Refresh intents`() {
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
        assertTrue(m.hasPagedOffset)
        assertTrue(m.contractImports.any { it.contains("PagingListState") })
        assertTrue(m.contractImports.any { it.contains("DEFAULT_FIRST_PAGE") })
        assertEquals("AppAiChatSessionRespVO", m.pagedStateItemContractRef)
        assertEquals("PageResultAppAiChatSessionRespVO", m.pagedConcreteInnerContractRef)
        assertTrue(m.stateFields.any { it.name == "items" && it.defaultLiteral == "emptyList()" })
        assertTrue(m.stateFields.any { it.name == "pagingError" })
        assertTrue(m.primaryPagedArgList.contains("pageNo = page"))
        assertTrue(m.primaryPagedArgList.contains("pageSize = size"))
        assertTrue(m.primaryPagedNonPageArgList.contains("topicId"))
        val names = m.intentInners.map { it.simpleName }
        assertTrue(names.indexOf("Refresh") < names.indexOf("LoadMore"))
        assertFalse(names.contains("AppAiChatGetSessionPage"))
        val h = m.useCaseHandlers.single()
        assertTrue(h.pagedBased)
        assertFalse(h.resultBased)
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
    fun `Get prefs UseCase with plain return maps to State via directReturnToState`() {
        val uc = UseCaseInfo(
            name = "GetUserIdUseCase",
            packageName = "p",
            path = "x",
            returnType = "String",
            parameters = emptyList(),
        )
        val m = PageTemplate(
            pageName = "Todolists",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        assertEquals("getUserId", m.stateFields.single().name)
        val h = m.useCaseHandlers.single()
        assertTrue(h.directReturnToState)
        assertEquals("getUserId", h.directStatePropertyName)
        assertFalse(h.resultBased)
        assertFalse(h.flowBased)
        assertFalse(h.unitEntityEchoToState)
        assertFalse(m.hasResultBasedHandler)
    }

    @Test
    fun `Update UseCase with Unit return echoes entity into State and generated handler`() {
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
        val f = m.stateFields.single()
        assertEquals("updatedUserSession", f.name)
        assertEquals(
            "com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserSessionEntity",
            f.typeFqn,
        )
        val h = m.useCaseHandlers.single()
        assertFalse(h.resultBased)
        assertFalse(h.flowBased)
        assertTrue(h.unitEntityEchoToState)
        assertEquals("updatedUserSession", h.unitEchoStatePropertyName)
        assertEquals("entity", h.unitEchoParamName)
        assertFalse(m.hasResultBasedHandler)
    }

    @Test
    fun `Insert UseCase with Unit return echoes entity into State under use case camelName`() {
        val uc = UseCaseInfo(
            name = "InsertUserSessionUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entity", "UserSessionEntity")),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        val f = m.stateFields.single()
        assertEquals("insertUserSession", f.name)
        assertEquals(
            "com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserSessionEntity",
            f.typeFqn,
        )
        val h = m.useCaseHandlers.single()
        assertTrue(h.unitEntityEchoToState)
        assertEquals("insertUserSession", h.unitEchoStatePropertyName)
        assertEquals("entity", h.unitEchoParamName)
    }

    @Test
    fun `InsertAll UseCase echoes entities List into State under use case camelName`() {
        val uc = UseCaseInfo(
            name = "InsertAllUserUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entities", "List<UserEntity>")),
        )
        val m = PageTemplate(
            pageName = "P",
            moduleName = "todo",
            modulePackage = "com.dqc.androidtest.feature.todo",
            useCases = listOf(uc),
            basePackage = "com.dqc.androidtest",
            baseClassPackages = BaseClassPackages(),
        ).toPageTemplateModel()
        val f = m.stateFields.single()
        assertEquals("insertAllUser", f.name)
        assertEquals(
            "List<com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserEntity>",
            f.typeFqn,
        )
        assertEquals("List<UserEntity>", f.typeContractRef)
        val h = m.useCaseHandlers.single()
        assertTrue(h.unitEntityEchoToState)
        assertEquals("insertAllUser", h.unitEchoStatePropertyName)
        assertEquals("entities", h.unitEchoParamName)
        assertTrue(
            m.contractImports.any {
                it == "import com.dqc.androidtest.feature.todo.generate.data.datasource.database.entity.UserEntity"
            },
        )
    }

    @Test
    fun `Delete UseCase with single entity param falls into echo branch`() {
        val uc = UseCaseInfo(
            name = "DeleteUserSessionUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entity", "UserSessionEntity")),
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
        assertTrue(h.unitEntityEchoToState)
        assertEquals("deleteUserSession", h.unitEchoStatePropertyName)
        assertEquals("entity", h.unitEchoParamName)
    }

    @Test
    fun `DeleteAll UseCase with no params keeps TODO handler and no state field`() {
        val uc = UseCaseInfo(
            name = "DeleteAllUserUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
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
        assertFalse(h.unitEntityEchoToState)
        assertFalse(h.directReturnToState)
        assertFalse(h.resultBased)
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
