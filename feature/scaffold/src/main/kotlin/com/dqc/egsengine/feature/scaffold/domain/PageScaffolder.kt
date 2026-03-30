package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.PageKotlinFileGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.effectiveBasePackage
import com.dqc.egsengine.feature.scaffold.domain.resolveScaffoldBaseClasses
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 页面脚手架领域服务 - 协调页面生成功能
 */
class PageScaffolder(
    private val configReader: EgsConfigReader,
    private val useCaseScanner: UseCaseScanner,
    private val diUpdater: FeatureDiUpdater,
) {
    private val logger = LoggerFactory.getLogger(PageScaffolder::class.java)

    /**
     * 生成页面脚手架
     */
    fun scaffold(
        projectRoot: File,
        moduleName: String,
        pageName: String,
        useCases: List<UseCaseInfo>,
        dryRun: Boolean = false,
    ): PageScaffoldResult {
        logger.info("Scaffolding page '$pageName' in module '$moduleName'")

        // 1. 读取配置
        val config = configReader.read(projectRoot)
        val basePackage = config.effectiveBasePackage()

        // 2. 构建模块包名
        val modulePackage = if (basePackage != null) {
            "$basePackage.feature.$moduleName"
        } else {
            "com.example.feature.$moduleName"
        }

        // 3. 解析基础类配置
        val baseClasses = config.resolveScaffoldBaseClasses(includeRetrofitProvider = true)

        // 4. 构建模板
        val template = PageTemplate(
            pageName = pageName.replaceFirstChar { it.uppercase() },
            moduleName = moduleName,
            modulePackage = modulePackage,
            useCases = useCases,
            basePackage = basePackage,
            baseClassPackages = baseClasses,
        )

        // 5. 预览生成内容
        val previewFiles = previewFiles(template)

        if (dryRun) {
            return PageScaffoldResult(
                pageName = template.pageName,
                moduleName = moduleName,
                files = previewFiles,
                dryRun = true,
            )
        }

        // 6. 检查目标目录是否已存在
        val camelName = template.pageName.replaceFirstChar { it.lowercase() }
        val screenDir = projectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/${modulePackage.replace(".", "/")}/presentation/screen/$camelName"
        )
        require(!screenDir.exists()) {
            "Page directory already exists: ${screenDir.relativeTo(projectRoot).path}"
        }

        // 7. 生成文件
        val generator = PageKotlinFileGenerator(template)
        val createdFiles = mutableListOf<File>()

        // Contract
        generator.generateContract().let { fileSpec ->
            val file = writeFile(projectRoot, moduleName, fileSpec)
            createdFiles.add(file)
            logger.debug("Created Contract: ${file.path}")
        }

        // ViewModel
        generator.generateViewModel().let { fileSpec ->
            val file = writeFile(projectRoot, moduleName, fileSpec)
            createdFiles.add(file)
            logger.debug("Created ViewModel: ${file.path}")
        }

        // Compose Screen
        generator.generateScreen().let { fileSpec ->
            val file = writeScreenFile(projectRoot, moduleName, fileSpec)
            createdFiles.add(file)
            logger.debug("Created Screen: ${file.path}")
        }

        // 8. 更新 DI Module
        diUpdater.updatePresentationModule(
            projectRoot = projectRoot,
            moduleName = moduleName,
            modulePackage = modulePackage,
            pageName = template.pageName,
            useCases = useCases,
        )

        logger.info("Successfully scaffolded page '${template.pageName}' in module '$moduleName'")

        return PageScaffoldResult(
            pageName = template.pageName,
            moduleName = moduleName,
            files = previewFiles,
            dryRun = false,
        )
    }

    /**
     * 预览将要生成的文件
     */
    private fun previewFiles(template: PageTemplate): List<GeneratedFileInfo> {
        val generator = PageKotlinFileGenerator(template)
        val files = mutableListOf<GeneratedFileInfo>()
        val modulePath = "feature/${template.moduleName}"

        // Contract
        generator.generateContract().let { fileSpec ->
            val path = "$modulePath/src/main/kotlin/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt"
            files.add(GeneratedFileInfo(path, fileSpec.toFixedString()))
        }

        // ViewModel
        generator.generateViewModel().let { fileSpec ->
            val path = "$modulePath/src/main/kotlin/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt"
            files.add(GeneratedFileInfo(path, fileSpec.toFixedString()))
        }

        // Compose Screen
        generator.generateScreen().let { fileSpec ->
            val path = "$modulePath/src/main/kotlin/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt"
            files.add(GeneratedFileInfo(path, fileSpec.toFixedString()))
        }

        return files
    }

    /**
     * 写入 Kotlin 文件
     */
    private fun writeFile(projectRoot: File, moduleName: String, fileSpec: com.squareup.kotlinpoet.FileSpec): File {
        val pkgPath = fileSpec.packageName.replace(".", "/")
        val file = projectRoot.resolve("feature/$moduleName/src/main/kotlin/$pkgPath/${fileSpec.name}.kt")
        file.parentFile.mkdirs()
        file.writeText(fileSpec.toFixedString())
        return file
    }

    /**
     * 写入 Screen Kotlin 文件（放在 presentation/screen 目录）
     */
    private fun writeScreenFile(projectRoot: File, moduleName: String, fileSpec: com.squareup.kotlinpoet.FileSpec): File {
        val pkgPath = fileSpec.packageName.replace(".", "/")
        val file = projectRoot.resolve("feature/$moduleName/src/main/kotlin/$pkgPath/${fileSpec.name}.kt")
        file.parentFile.mkdirs()
        file.writeText(fileSpec.toFixedString())
        return file
    }

    /** camelCase/PascalCase -> snake_case，如 TaskDetail -> task_detail */
    private fun toSnakeCase(s: String): String =
        s.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}

/**
 * FileSpec 扩展方法，修复 data 关键字转义、移除冗余 public
 */
/** camelCase/PascalCase → snake_case，如 TaskDetail → task_detail */
private fun String.toSnakeCase(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

private fun com.squareup.kotlinpoet.FileSpec.toFixedString(): String =
    toString()
        .replace("`data`", "data")
        .replace(Regex("""(?m)^(\s*)public """), "$1")
