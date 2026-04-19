package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpPageTemplateModelMapperTest {

    @Test
    fun `Intent params use short primitives not kotlin dot prefixed`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "p",
            path = "x",
            returnType = "kotlinx.coroutines.flow.Flow<Unit>",
            parameters = listOf(
                UseCaseParam("topicId", "kotlin.Long?"),
                UseCaseParam("pageNo", "kotlin.Int"),
                UseCaseParam("pageSize", "kotlin.Int"),
            ),
        )
        val pt = PageTemplate(
            pageName = "TodoDetails",
            moduleName = "todolist",
            modulePackage = "org.mifos.feature.todolist",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        @Suppress("UNCHECKED_CAST")
        val intentInners = m["intentInners"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val params = intentInners.single()["params"] as List<Map<String, Any?>>
        assertEquals("Long?", params[0]["kotlinType"])
        assertEquals("Int", params[1]["kotlinType"])
        assertEquals("Int", params[2]["kotlinType"])
    }

    @Test
    fun `stateFields and contractImports for Result with FQN inner type`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<com.example.domain.model.PageResultAppAiChatSessionRespVO>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "Todolist",
            moduleName = "todolist",
            modulePackage = "org.mifos.feature.todolist",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        @Suppress("UNCHECKED_CAST")
        val fields = m["stateFields"] as List<Map<String, Any?>>
        assertEquals(1, fields.size)
        assertEquals("appAiChatGetSessionPage", fields[0]["name"])
        assertEquals("com.example.domain.model.PageResultAppAiChatSessionRespVO", fields[0]["typeFqn"])
        @Suppress("UNCHECKED_CAST")
        val imports = m["contractImports"] as List<String>
        assertTrue(imports.any { it.contains("com.example.domain.model.PageResultAppAiChatSessionRespVO") })
    }

    @Test
    fun `swagger style Result PageResult Item matches PagePagingDetector offset auto`() {
        val returnType = "Result<PageResult<User>>"
        assertTrue(PagePagingDetector.isOffsetPageResultUseCase(returnType, "auto"))
    }

    @Test
    fun `nested Result PageResult T expands to offset paging state fields`() {
        val uc = UseCaseInfo(
            name = "NestedUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<PageResult<com.example.domain.model.RowVO>>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "X",
            moduleName = "m",
            modulePackage = "org.example.feature.m",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        assertEquals(true, m["hasPagedOffset"])
        @Suppress("UNCHECKED_CAST")
        val fields = m["stateFields"] as List<Map<String, Any?>>
        assertEquals(8, fields.size)
        val names = fields.map { it["name"] as String }
        assertTrue(names.contains("items"))
        assertTrue(names.contains("page"))
        assertTrue(names.contains("endReached"))
    }

    @Test
    fun `resultClassFqn defaults to template core network Result when unset`() {
        val uc = UseCaseInfo(
            name = "FooUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<Bar>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "T",
            moduleName = "todo",
            modulePackage = "com.dqc.example.feature.todo",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        assertEquals("template.core.base.network.domain.Result", m["resultClassFqn"])
    }

    @Test
    fun `resultClassFqn uses baseClassPackages resultClass when set`() {
        val uc = UseCaseInfo(
            name = "FooUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<Bar>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "T",
            moduleName = "m",
            modulePackage = "org.example.feature.m",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(
                resultClass = "com.myapp.feature.base.domain.result.Result",
            ),
        )
        val m = pt.toKmpPageTemplateMap()
        assertEquals("com.myapp.feature.base.domain.result.Result", m["resultClassFqn"])
    }

    @Test
    fun `PageResult return type is resultBased not flowBased`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "p",
            path = "x",
            returnType = "Result<PageResultAppAiChatSessionRespVO>",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todolist",
            modulePackage = "org.mifos.feature.todolist",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        @Suppress("UNCHECKED_CAST")
        val handlers = m["useCaseHandlers"] as List<Map<String, Any?>>
        assertEquals(true, handlers.single()["resultBased"])
        assertEquals(false, handlers.single()["flowBased"])
    }

    @Test
    fun `end-to-end rendered Contract and ViewModel for Result UseCase`() {
        val uc = UseCaseInfo(
            name = "AppAiChatGetSessionPageUseCase",
            packageName = "com.dqc.example.feature.todolist.domain.usecase",
            path = "x",
            returnType = "Result<PageResultAppAiChatSessionRespVO>",
            parameters = listOf(
                UseCaseParam("topicId", "Long?"),
                UseCaseParam("pageNo", "Int"),
                UseCaseParam("pageSize", "Int"),
            ),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todolist",
            modulePackage = "com.dqc.example.feature.todolist",
            useCases = listOf(uc),
            basePackage = "com.dqc.example",
            baseClassPackages = BaseClassPackages(),
        )
        val engine = TemplateEngine(TemplateRegistry())
        val renderer = KmpPageTemplateRenderer(engine, pt, "src/commonMain/kotlin")

        val contract = renderer.renderContract()
        assertTrue(contract.contains("PagingListState<AppAiChatSessionRespVO>"), contract)
        assertTrue(contract.contains("override val items:"), contract)
        assertTrue(contract.contains("override fun copyPaging("), contract)

        val vm = renderer.renderViewModel()
        assertTrue(vm.contains("private fun loadPage("), vm)
        assertTrue(vm.contains("runPagedLoad<AppAiChatSessionRespVO>"), vm)
        assertTrue(vm.contains("PageResult("), vm)
        assertTrue(vm.contains("is Result.Success"), vm)
        assertTrue(vm.contains("is Result.Failure"), vm)
        assertFalse(vm.contains("// TODO: map result to State"), "Should NOT contain TODO placeholder")
        assertTrue(
            vm.contains("template.core.base.network.domain.Result"),
            "Should import template core network Result",
        )
        assertTrue(
            contract.contains("com.dqc.example.feature.todolist.generate.domain.model.AppAiChatSessionRespVO"),
            "Contract should import item VO from generate.domain.model",
        )
    }

    @Test
    fun `Get prefs UseCase with plain String return uses directReturnToState in ViewModel`() {
        val uc = UseCaseInfo(
            name = "GetUserIdUseCase",
            packageName = "p",
            path = "x",
            returnType = "String",
            parameters = emptyList(),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todolist",
            modulePackage = "org.mifos.feature.todolist",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        @Suppress("UNCHECKED_CAST")
        val handlers = m["useCaseHandlers"] as List<Map<String, Any?>>
        val h = handlers.single()
        assertEquals(true, h["directReturnToState"])
        assertEquals("getUserId", h["directStatePropertyName"])

        val engine = TemplateEngine(TemplateRegistry())
        val renderer = KmpPageTemplateRenderer(engine, pt, "src/commonMain/kotlin")
        val vm = renderer.renderViewModel()
        assertTrue(vm.contains("updateState { copy(getUserId = ret, error = null) }"), vm)
        assertFalse(vm.contains("// TODO: map result to State"), vm)
    }

    @Test
    fun `Update UseCase with Unit return echoes entity into stateFields and ViewModel`() {
        val uc = UseCaseInfo(
            name = "UpdateUserSessionUseCase",
            packageName = "p",
            path = "x",
            returnType = "Unit",
            parameters = listOf(UseCaseParam("entity", "UserSessionEntity")),
        )
        val pt = PageTemplate(
            pageName = "Todolists",
            moduleName = "todolist",
            modulePackage = "org.mifos.feature.todolist",
            useCases = listOf(uc),
            basePackage = null,
            baseClassPackages = BaseClassPackages(),
        )
        val m = pt.toKmpPageTemplateMap()
        @Suppress("UNCHECKED_CAST")
        val fields = m["stateFields"] as List<Map<String, Any?>>
        assertEquals("updatedUserSession", fields.single()["name"])
        @Suppress("UNCHECKED_CAST")
        val handlers = m["useCaseHandlers"] as List<Map<String, Any?>>
        val h = handlers.single()
        assertEquals(true, h["unitEntityEchoToState"])
        assertEquals("updatedUserSession", h["unitEchoStatePropertyName"])
        assertEquals("entity", h["unitEchoParamName"])
        assertEquals(false, h["resultBased"])

        val engine = TemplateEngine(TemplateRegistry())
        val renderer = KmpPageTemplateRenderer(engine, pt, "src/commonMain/kotlin")
        val vm = renderer.renderViewModel()
        assertTrue(vm.contains("updateState { copy(updatedUserSession = entity, error = null) }"), vm)
        assertFalse(vm.contains("// TODO: map result to State"), vm)
    }
}
