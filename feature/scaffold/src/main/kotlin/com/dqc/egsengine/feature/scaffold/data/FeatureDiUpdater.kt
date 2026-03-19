package com.dqc.egsengine.feature.scaffold.data

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
    ): Boolean {
        val presentationModuleFile = findPresentationModuleFile(projectRoot, moduleName, modulePackage)
            ?: createPresentationModuleFile(projectRoot, moduleName, modulePackage)

        return updateModuleFile(presentationModuleFile, pageName, useCases, modulePackage)
    }

    /**
     * 查找现有的 PresentationModule 文件
     */
    private fun findPresentationModuleFile(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
    ): File? {
        val moduleDir = projectRoot.resolve("feature/$moduleName/src/main/kotlin")
        if (!moduleDir.exists()) return null

        // 尝试在不同位置查找
        val possiblePaths = listOf(
            moduleDir.resolve(modulePackage.replace(".", "/") + "/presentation/PresentationModule.kt"),
            moduleDir.resolve(modulePackage.replace(".", "/") + "/di/PresentationModule.kt"),
            moduleDir.resolve("com/dqc/egsengine/feature/$moduleName/presentation/PresentationModule.kt"),
        )

        return possiblePaths.firstOrNull { it.exists() }
    }

    /**
     * 创建新的 PresentationModule 文件
     */
    private fun createPresentationModuleFile(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
    ): File {
        val moduleDir = projectRoot.resolve("feature/$moduleName/src/main/kotlin")
        val pkgPath = modulePackage.replace(".", "/")
        val file = moduleDir.resolve("$pkgPath/presentation/PresentationModule.kt")

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
        val viewModelImport = "import $modulePackage.presentation.fragment.$camelName.${pascalName}ViewModel"
        val updatedContent = if (!content.contains(viewModelImport)) {
            content.replace(
                "import org.koin.dsl.module",
                "import org.koin.dsl.module\n$viewModelImport"
            )
        } else {
            content
        }

        // 添加 viewModelOf 绑定
        val viewModelBinding = if (useCases.isEmpty()) {
            "    viewModelOf(::$pascalName" + "ViewModel)"
        } else {
            val params = useCases.joinToString(", ") { "get()" }
            "    viewModel { $pascalName" + "ViewModel($params) }"
        }

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
    ): String {
        val pascalName = pageName.replaceFirstChar { it.uppercase() }
        val camelName = pageName.replaceFirstChar { it.lowercase() }

        val binding = if (useCases.isEmpty()) {
            "viewModelOf(::$pascalName" + "ViewModel)"
        } else {
            val params = useCases.joinToString(", ") { "get()" }
            "viewModel { $pascalName" + "ViewModel($params) }"
        }

        return """
            // 将添加到 PresentationModule.kt:
            import $modulePackage.presentation.fragment.$camelName.${pascalName}ViewModel
            
            internal val presentationModule: Module = module {
                $binding
            }
        """.trimIndent()
    }
}
