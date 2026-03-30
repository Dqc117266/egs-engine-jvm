package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.KotlinFileGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.XmlTemplateGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.toFixedString
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.squareup.kotlinpoet.FileSpec
import org.slf4j.LoggerFactory
import java.io.File

class AndroidModuleGenerator(
    private val settingsUpdater: SettingsGradleUpdater,
) : PlatformModuleGenerator {

    private val logger = LoggerFactory.getLogger(AndroidModuleGenerator::class.java)

    override val platform: Platform = Platform.ANDROID

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val template = toModuleTemplate(moduleName, config)
        return previewFromTemplate(template)
    }

    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        val template = toModuleTemplate(moduleName, config)
        val created = mutableListOf<File>()
        val subProjectRoot = projectRoot.resolve(config.path)

        for (entry in previewFromTemplate(template)) {
            val file = subProjectRoot.resolve(entry.path)
            file.parentFile.mkdirs()

            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }

            created.add(file)
            logger.debug("Created: {}", entry.path)
        }

        logger.info("Generated {} files for Android module '{}'", created.size, moduleName)
        return created
    }

    override fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        val subProjectRoot = projectRoot.resolve(config.path)
        settingsUpdater.update(subProjectRoot, moduleName)
    }

    fun previewFromTemplate(template: ModuleTemplate): List<GeneratedFile> {
        val kotlinGen = KotlinFileGenerator(template)
        val xmlGen = XmlTemplateGenerator(template)
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/${template.name}"
        val isAndroid = template.isAndroid

        files.add(GeneratedFile("$moduleDir/build.gradle.kts", generateBuildFile(template)))

        files.addKt(moduleDir, kotlinGen.generateRootKoinModule())
        files.addKt(moduleDir, kotlinGen.generateDataModule())
        files.addKt(moduleDir, kotlinGen.generateDomainModule())
        files.addKt(moduleDir, kotlinGen.generatePresentationModule())
        files.addKt(moduleDir, kotlinGen.generateRepositoryInterface())
        files.addKt(moduleDir, kotlinGen.generateRepositoryImpl())
        files.addKt(moduleDir, kotlinGen.generateViewModel())

        if (isAndroid) {
            kotlinGen.generateContract()?.let { files.addKt(moduleDir, it) }
            kotlinGen.generateNavigationRoute()?.let { files.addKt(moduleDir, it) }

            files.add(
                GeneratedFile(
                    "$moduleDir/src/main/AndroidManifest.xml",
                    xmlGen.generateAndroidManifest(),
                ),
            )
        }

        return files
    }

    fun generateFromTemplate(projectRoot: File, template: ModuleTemplate): List<File> {
        val created = mutableListOf<File>()

        for (entry in previewFromTemplate(template)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()

            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }

            created.add(file)
            logger.debug("Created: {}", entry.path)
        }

        logger.info("Generated {} files for module '{}'", created.size, template.name)
        return created
    }

    private fun MutableList<GeneratedFile>.addKt(moduleDir: String, fileSpec: FileSpec) {
        val pkgPath = fileSpec.packageName.replace('.', '/')
        val path = "$moduleDir/src/main/kotlin/$pkgPath/${fileSpec.name}.kt"
        add(GeneratedFile(path, fileSpec.toFixedString()))
    }

    private fun generateBuildFile(template: ModuleTemplate): String = buildString {
        val isAndroid = template.isAndroid

        appendLine("plugins {")
        if (template.conventionPluginId != null) {
            appendLine("    id(\"${template.conventionPluginId}\")")
        } else {
            appendLine("    id(\"org.jetbrains.kotlin.jvm\")")
        }
        appendLine("}")

        if (isAndroid && template.namespace != null) {
            appendLine()
            appendLine("android {")
            appendLine("    namespace = \"${template.namespace}\"")
            appendLine("}")
        }
    }

    companion object {
        fun toModuleTemplate(moduleName: String, config: SubProjectConfig): ModuleTemplate {
            val normalizedModule = moduleName.replace("-", "").replace("_", "")
            val featurePackage = "${config.basePackage}.feature.$normalizedModule"
            val isAndroid = config.platform in setOf(Platform.ANDROID, Platform.KMP_ANDROID)
            val namespace = if (isAndroid) featurePackage else null

            return ModuleTemplate(
                name = moduleName,
                packageName = featurePackage,
                conventionPluginId = config.conventionPluginId,
                layers = config.moduleStructure?.layers ?: listOf("data", "domain", "presentation"),
                hasRes = config.moduleStructure?.hasRes ?: false,
                namespace = namespace,
                projectType = config.platform.name,
                basePackage = config.basePackage,
                baseClassPackages = resolveBaseClasses(config),
            )
        }

        private fun resolveBaseClasses(config: SubProjectConfig): BaseClassPackages {
            val overrides = config.scaffoldOverrides
            val baseViewModel = overrides?.baseViewModelFqn
                ?: config.baseClasses.find { it.name == "BaseViewModel" }?.let { "${it.packageName}.BaseViewModel" }
            val baseFragment = overrides?.baseFragmentFqn
                ?: config.baseClasses.find { it.name == "BaseFragment" }?.let { "${it.packageName}.BaseFragment" }
            val bp = overrides?.basePackage ?: config.basePackage

            return BaseClassPackages(
                baseViewModel = baseViewModel,
                baseFragment = baseFragment,
                resultClass = bp?.let { "$it.feature.base.domain.result.Result" },
                retrofitProvider = bp?.let { "$it.feature.common.network.DynamicRetrofitProvider" },
            )
        }
    }
}
