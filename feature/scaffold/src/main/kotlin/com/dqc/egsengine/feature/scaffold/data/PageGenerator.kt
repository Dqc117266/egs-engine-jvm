package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import com.dqc.egsengine.feature.templateengine.model.BaseClassPackagesModel
import com.dqc.egsengine.feature.templateengine.model.PageIntentInnerModel
import com.dqc.egsengine.feature.templateengine.model.PageStateFieldModel
import com.dqc.egsengine.feature.templateengine.model.PageTemplateModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseHandlerModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseParamModel
import java.io.File

class PageGenerator(
    private val templateEngine: TemplateEngine = TemplateEngine(TemplateRegistry()),
) {

    fun preview(projectRoot: File, template: PageTemplate, projectType: String): List<GeneratedFileInfo> {
        val model = template.toModel(projectType)
        val templateRoot = if (projectType in setOf("KMP", "KMP_ANDROID")) "kmp/page" else "android/page"
        val sourceRoot = if (projectType in setOf("KMP", "KMP_ANDROID")) {
            "feature/${template.moduleName}/src/commonMain/kotlin/${template.modulePackage.toPath()}"
        } else {
            "feature/${template.moduleName}/src/main/kotlin/${template.modulePackage.toPath()}"
        }
        val screenPath = model.screenPkg.removePrefix("${template.modulePackage}.").replace('.', '/')

        return listOf(
            render(projectRoot, "$templateRoot/PageContract.kt.ftl", model, "$sourceRoot/$screenPath/${model.pascalName}Contract.kt"),
            render(projectRoot, "$templateRoot/PageViewModel.kt.ftl", model, "$sourceRoot/$screenPath/${model.pascalName}ViewModel.kt"),
            render(projectRoot, "$templateRoot/PageScreen.kt.ftl", model, "$sourceRoot/$screenPath/${model.pascalName}Screen.kt"),
        )
    }

    fun generate(projectRoot: File, template: PageTemplate, projectType: String): List<File> = preview(projectRoot, template, projectType).map { generated ->
        projectRoot.resolve(generated.path).also { file ->
            file.parentFile.mkdirs()
            file.writeText(generated.content)
        }
    }

    private fun render(projectRoot: File, templateName: String, model: PageTemplateModel, path: String): GeneratedFileInfo = GeneratedFileInfo(
        path = path,
        content = templateEngine.render(templateName, model, projectRoot),
    )

    private fun PageTemplate.toModel(projectType: String): PageTemplateModel {
        val pascal = pageName
        val camel = pageName.replaceFirstChar { it.lowercase() }
        val isKmp = projectType in setOf("KMP", "KMP_ANDROID")
        val screenPkg = if (isKmp) {
            "$modulePackage.presentation.$camel"
        } else {
            "$modulePackage.presentation.screen.$camel"
        }
        val modelPackage = "$modulePackage.domain.model"
        val resultPackage = basePackage?.let { "$it.feature.base.domain.result" }
            ?: "com.example.feature.base.domain.result"
        val uiContractPackage = baseClassPackages.baseViewModel?.substringBeforeLast(".")
            ?: basePackage?.let { "$it.feature.base.presentation.viewmodel" }
            ?: "com.example.feature.base.presentation.viewmodel"
        val baseViewModel = baseClassPackages.baseViewModel
            ?: basePackage?.let { "$it.feature.base.presentation.viewmodel.BaseViewModel" }
            ?: "com.example.feature.base.presentation.viewmodel.BaseViewModel"
        val useCaseModels = useCases.map { it.toModel(modelPackage) }
        val stateFields = useCases.mapNotNull { it.toStateField(modelPackage) }
        val contractImports = buildContractImports(useCases, stateFields)
        val intentInners = useCaseModels.map { useCase ->
            PageIntentInnerModel(
                simpleName = useCase.intentName,
                emptyParams = useCase.parameters.isEmpty(),
                params = useCase.parameters,
            )
        }
        val handlers = useCases.map { it.toHandler() }
        val hasResultBasedHandler = handlers.any { it.resultBased }

        return PageTemplateModel(
            pascalName = pascal,
            camelName = camel,
            layoutSnakeName = pascal.toSnakeCase(),
            modulePackage = modulePackage,
            screenPkg = screenPkg,
            screenDirPkg = screenPkg,
            modelPackage = modelPackage,
            resultPackage = resultPackage,
            uiContractPackage = uiContractPackage,
            baseClasses = baseClassPackages.toModel(),
            basePackage = basePackage,
            useCases = useCaseModels,
            hasUseCases = useCaseModels.isNotEmpty(),
            hasBaseViewModel = true,
            baseViewModelIsAndroidX = false,
            baseViewModelImport = baseViewModel,
            baseViewModelSimpleName = baseViewModel.substringAfterLast("."),
            stateFields = stateFields,
            contractImports = contractImports,
            intentInners = intentInners,
            useCaseHandlers = handlers,
            hasResultBasedHandler = hasResultBasedHandler,
            resultClassFqn = "$resultPackage.Result",
        )
    }

    private fun UseCaseInfo.toModel(modelPackage: String): PageUseCaseModel = PageUseCaseModel(
        name = name,
        camelName = camelName,
        packageName = packageName,
        returnType = returnType,
        parameters = parameters.map { it.toModel(modelPackage) },
        intentName = name.removeSuffix("UseCase"),
        handlerName = "handle${name.removeSuffix("UseCase").replaceFirstChar { it.uppercase() }}",
    )

    private fun UseCaseParam.toModel(modelPackage: String): PageUseCaseParamModel {
        val normalized = type.normalizeKotlinType(modelPackage)
        return PageUseCaseParamModel(
            name = name,
            type = type,
            kotlinType = normalized.reference,
            kotlinTypeContractRef = normalized.reference,
            placeholderValue = placeholderValue,
        )
    }

    private fun UseCaseInfo.toStateField(modelPackage: String): PageStateFieldModel? {
        val innerType = returnType?.resultInnerType() ?: returnType ?: return null
        if (innerType == "Unit") return null
        val normalized = innerType.normalizeKotlinType(modelPackage)
        return PageStateFieldModel(
            name = camelName,
            typeFqn = normalized.importFqn ?: normalized.reference,
            typeContractRef = normalized.reference,
            nullable = true,
        )
    }

    private fun UseCaseInfo.toHandler(): PageUseCaseHandlerModel {
        val resultBased = returnType?.contains("Result<") == true
        val flowBased = returnType?.contains("Flow<") == true
        return PageUseCaseHandlerModel(
            intentSimpleName = name.removeSuffix("UseCase"),
            handlerName = "handle${name.removeSuffix("UseCase").replaceFirstChar { it.uppercase() }}",
            useCaseCamel = camelName,
            hasParams = parameters.isNotEmpty(),
            paramPassArgs = parameters.joinToString(", ") { "${it.name} = ${it.name}" },
            showLoading = returnType?.contains("SseEmitter") != true,
            resultBased = resultBased,
            flowBased = flowBased,
            directReturnToState = !resultBased && !flowBased && returnType != null && returnType != "Unit",
            directStatePropertyName = camelName,
        )
    }

    private fun buildContractImports(useCases: List<UseCaseInfo>, stateFields: List<PageStateFieldModel>): List<String> {
        val paramImports = useCases
            .flatMap { it.parameters }
            .mapNotNull { it.type.normalizeKotlinType("").importFqn }
        val stateImports = stateFields.mapNotNull { field ->
            field.typeFqn.takeIf { "." in it && !it.startsWith("kotlin.") }
        }
        return (paramImports + stateImports)
            .distinct()
            .sorted()
            .map { "import $it" }
    }

    private fun BaseClassPackages.toModel(): BaseClassPackagesModel = BaseClassPackagesModel(
        baseViewModel = baseViewModel,
        baseFragment = baseFragment,
        resultClass = resultClass,
        retrofitProvider = retrofitProvider,
    )

    private data class KotlinTypeRef(
        val reference: String,
        val importFqn: String?,
    )

    private fun String.resultInnerType(): String? = Regex("""Result<(.+)>""").find(this)?.groupValues?.get(1)

    private fun String.normalizeKotlinType(defaultPackage: String): KotlinTypeRef {
        val nullable = endsWith("?")
        val base = removeSuffix("?")
        val normalizedBase = when {
            base.startsWith("List<") && base.endsWith(">") -> {
                val inner = base.substringAfter("List<").substringBeforeLast(">")
                val innerRef = inner.normalizeKotlinType(defaultPackage)
                "List<${innerRef.reference}>"
            }
            base in setOf("Boolean", "Int", "Long", "String", "Double", "Float", "Unit") -> base
            base.endsWith("ApiModel") -> base.removeSuffix("ApiModel").substringAfterLast(".")
            "." in base -> base.substringAfterLast(".")
            else -> base
        }
        val importFqn = when {
            "." in base && !base.startsWith("kotlin.") -> base.removeSuffix("?")
            "." !in base && defaultPackage.isNotEmpty() && base !in setOf("Boolean", "Int", "Long", "String", "Double", "Float", "Unit") ->
                "$defaultPackage.$base"
            else -> null
        }
        return KotlinTypeRef(
            reference = normalizedBase + if (nullable) "?" else "",
            importFqn = importFqn,
        )
    }

    private fun String.toPath(): String = replace('.', '/')

    private fun String.toSnakeCase(): String = replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
