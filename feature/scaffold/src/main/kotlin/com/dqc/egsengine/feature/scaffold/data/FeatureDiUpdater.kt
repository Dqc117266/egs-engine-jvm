package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.init.data.GradleSourceRoots
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 功能模块 DI 更新器 - 更新 Koin Module 添加 ViewModel 绑定
 */
class FeatureDiUpdater {
    private val logger = LoggerFactory.getLogger(FeatureDiUpdater::class.java)

    /**
     * 更新指定模块的 PresentationModule，添加 ViewModel 绑定
     */
    fun updatePresentationModule(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        pageName: String,
        useCases: List<UseCaseInfo>,
        kotlinRootRel: String = "src/main/kotlin",
        /** KMP uses `presentation.<pageCamel>`; Android legacy uses `presentation.fragment.<pageCamel>`. */
        useKmpPresentationLayout: Boolean = false,
    ): Boolean {
        val presentationModuleFile = findPresentationModuleFile(projectRoot, moduleName, modulePackage)
            ?: createPresentationModuleFile(projectRoot, moduleName, modulePackage, kotlinRootRel)

        return updateModuleFile(
            presentationModuleFile,
            pageName,
            useCases,
            modulePackage,
            useKmpPresentationLayout,
        )
    }

    /**
     * 查找现有的 PresentationModule 文件
     */
    private fun findPresentationModuleFile(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
    ): File? {
        val moduleDir = projectRoot.resolve("feature/$moduleName")
        val kotlinRoots = GradleSourceRoots.orderedKotlinRoots(moduleDir)
        val roots = if (kotlinRoots.isNotEmpty()) kotlinRoots else listOf(moduleDir.resolve("src/main/kotlin"))

        for (kotlinRoot in roots) {
            val possiblePaths = listOf(
                kotlinRoot.resolve(modulePackage.replace(".", "/") + "/presentation/PresentationModule.kt"),
                kotlinRoot.resolve(modulePackage.replace(".", "/") + "/di/PresentationModule.kt"),
                kotlinRoot.resolve("com/dqc/egsengine/feature/$moduleName/presentation/PresentationModule.kt"),
            )
            val found = possiblePaths.firstOrNull { it.exists() }
            if (found != null) return found
        }
        return null
    }

    /**
     * 创建新的 PresentationModule 文件
     */
    private fun createPresentationModuleFile(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        kotlinRootRel: String,
    ): File {
        val moduleDir = projectRoot.resolve("feature/$moduleName")
        val kotlinRoot = moduleDir.resolve(kotlinRootRel)
        val pkgPath = modulePackage.replace(".", "/")
        val file = kotlinRoot.resolve("$pkgPath/presentation/PresentationModule.kt")

        file.parentFile.mkdirs()

        val content = buildString {
            appendLine("package $modulePackage.presentation")
            appendLine()
            appendLine("import org.koin.core.module.Module")
            appendLine("import org.koin.dsl.module")
            appendLine()
            appendLine("internal val presentationModule: Module = module {")
            appendLine("    // ViewModels will be registered here")
            appendLine("}")
        }

        file.writeText(content)
        logger.info("Created PresentationModule: ${file.path}")
        return file
    }

    /**
     * 更新 Module 文件内容
     */
    private fun updateModuleFile(
        file: File,
        pageName: String,
        useCases: List<UseCaseInfo>,
        modulePackage: String,
        useKmpPresentationLayout: Boolean,
    ): Boolean {
        val content = file.readText()
        val pascalName = pageName.replaceFirstChar { it.uppercase() }
        val camelName = pageName.replaceFirstChar { it.lowercase() }

        // 检查是否已存在
        if (content.contains("${pascalName}ViewModel")) {
            logger.warn("${pascalName}ViewModel already registered in ${file.name}")
            return false
        }

        // 添加 import
        val viewModelImport = if (useKmpPresentationLayout) {
            "import $modulePackage.presentation.$camelName.${pascalName}ViewModel"
        } else {
            "import $modulePackage.presentation.fragment.$camelName.${pascalName}ViewModel"
        }
        val viewModelOfImport = "import org.koin.core.module.dsl.viewModelOf"
        var updatedContent = content
        if (!updatedContent.contains(viewModelImport)) {
            updatedContent = updatedContent.replace(
                "import org.koin.dsl.module",
                "import org.koin.dsl.module\n$viewModelImport"
            )
        }
        if (!updatedContent.contains("viewModelOf")) {
            updatedContent = updatedContent.replace(
                "import org.koin.dsl.module",
                "import org.koin.dsl.module\n$viewModelOfImport"
            )
        }

        // 始终使用 viewModelOf（ViewModel 空参或由 Koin 自动注入）
        val viewModelBinding = "    viewModelOf(::$pascalName" + "ViewModel)"

        // 查找 module 代码块并插入
        val moduleRegex = Regex("""(module\s*\{[^}]*)(\s*\})""")
        val finalContent = if (moduleRegex.containsMatchIn(updatedContent)) {
            updatedContent.replace(moduleRegex) { match ->
                val body = match.groupValues[1]
                val closing = match.groupValues[2]
                if (body.contains("// ViewModels will be registered here")) {
                    body.replace("// ViewModels will be registered here", viewModelBinding) + closing
                } else {
                    body + "\n$viewModelBinding" + closing
                }
            }
        } else {
            updatedContent
        }

        file.writeText(finalContent)
        logger.info("Updated ${file.name} with ${pascalName}ViewModel binding")
        return true
    }

    /**
     * 预览将要进行的更新
     */
    fun previewUpdate(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        pageName: String,
        useCases: List<UseCaseInfo>,
        useKmpPresentationLayout: Boolean = false,
    ): String {
        val pascalName = pageName.replaceFirstChar { it.uppercase() }
        val camelName = pageName.replaceFirstChar { it.lowercase() }

        val binding = "viewModelOf(::$pascalName" + "ViewModel)"
        val importLine = if (useKmpPresentationLayout) {
            "import $modulePackage.presentation.$camelName.${pascalName}ViewModel"
        } else {
            "import $modulePackage.presentation.fragment.$camelName.${pascalName}ViewModel"
        }

        return """
            // 将添加到 PresentationModule.kt:
            $importLine
            
            internal val presentationModule: Module = module {
                $binding
            }
        """.trimIndent()
    }
}
