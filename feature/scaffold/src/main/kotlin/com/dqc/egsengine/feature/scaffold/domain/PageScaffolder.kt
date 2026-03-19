package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.template.PageKotlinFileGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
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
        val basePackage = config.basePackage

        // 2. 构建模块包名
        val modulePackage = if (basePackage != null) {
            "$basePackage.feature.$moduleName"
        } else {
            "com.example.feature.$moduleName"
        }

        // 3. 解析基础类配置
        val baseClasses = resolveBaseClassPackages(config.baseClasses, basePackage)

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

        // 6. 检查目标目录是否存在
        val fragmentDir = projectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/${modulePackage.replace(".", "/")}/presentation/fragment/${pageName.lowercase()}"
        )
        require(!fragmentDir.exists()) {
            "Page directory already exists: ${fragmentDir.relativeTo(projectRoot).path}"
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

        // Fragment (if BaseFragment available)
        generator.generateFragment()?.let { fileSpec ->
            val file = writeFile(projectRoot, moduleName, fileSpec)
            createdFiles.add(file)
            logger.debug("Created Fragment: ${file.path}")
        }

        // ViewModel
        generator.generateViewModel().let { fileSpec ->
            val file = writeFile(projectRoot, moduleName, fileSpec)
            createdFiles.add(file)
            logger.debug("Created ViewModel: ${file.path}")
        }

        // Layout XML
        val layoutContent = generator.generateLayout()
        val layoutFile = projectRoot.resolve(
            "feature/$moduleName/src/main/res/layout/fragment_${pageName.lowercase()}.xml"
        )
        layoutFile.parentFile.mkdirs()
        layoutFile.writeText(layoutContent)
        createdFiles.add(layoutFile)
        logger.debug("Created Layout: ${layoutFile.path}")

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

        // Fragment
        generator.generateFragment()?.let { fileSpec ->
            val path = "$modulePath/src/main/kotlin/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt"
            files.add(GeneratedFileInfo(path, fileSpec.toFixedString()))
        }

        // ViewModel
        generator.generateViewModel().let { fileSpec ->
            val path = "$modulePath/src/main/kotlin/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt"
            files.add(GeneratedFileInfo(path, fileSpec.toFixedString()))
        }

        // Layout
        val layoutPath = "$modulePath/src/main/res/layout/fragment_${template.pageName.lowercase()}.xml"
        files.add(GeneratedFileInfo(layoutPath, generator.generateLayout()))

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
     * 解析基础类包配置
     */
    private fun resolveBaseClassPackages(
        baseClasses: List<com.dqc.egsengine.feature.init.domain.model.BaseClassInfo>,
        basePackage: String?,
    ): BaseClassPackages {
        fun findBaseClass(name: String): String? =
            baseClasses.find { it.name == name }?.let { "${it.packageName}.$name" }

        return BaseClassPackages(
            baseViewModel = findBaseClass("BaseViewModel"),
            baseFragment = findBaseClass("BaseFragment"),
            resultClass = basePackage?.let { "$it.feature.base.domain.result.Result" },
            retrofitProvider = basePackage?.let { "$it.feature.common.network.DynamicRetrofitProvider" },
        )
    }
}

/**
 * FileSpec 扩展方法，修复 data 关键字转义问题
 */
private fun com.squareup.kotlinpoet.FileSpec.toFixedString(): String =
    toString().replace("`data`", "data")
