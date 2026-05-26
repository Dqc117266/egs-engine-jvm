package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.PageGenerator
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
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
    private val generator: PageGenerator = PageGenerator(),
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
        val previewFiles = generator.preview(projectRoot, template, config.projectType)

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
        val sourceSet = if (config.projectType in setOf("KMP", "KMP_ANDROID")) "commonMain" else "main"
        val screenPath = if (config.projectType in setOf("KMP", "KMP_ANDROID")) {
            "presentation/$camelName"
        } else {
            "presentation/screen/$camelName"
        }
        val screenDir = projectRoot.resolve(
            "feature/$moduleName/src/$sourceSet/kotlin/${modulePackage.replace(".", "/")}/$screenPath"
        )
        require(!screenDir.exists()) {
            "Page directory already exists: ${screenDir.relativeTo(projectRoot).path}"
        }

        // 7. 生成文件
        val createdFiles = generator.generate(projectRoot, template, config.projectType)
        createdFiles.forEach { file -> logger.debug("Created page file: ${file.path}") }

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

    /** camelCase/PascalCase -> snake_case，如 TaskDetail -> task_detail */
    private fun toSnakeCase(s: String): String =
        s.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
