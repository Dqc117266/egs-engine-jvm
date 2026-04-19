package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.KotlinModuleTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

class AndroidModuleGenerator(
    private val settingsUpdater: SettingsGradleUpdater,
    private val templateEngine: TemplateEngine,
) : PlatformModuleGenerator {

    private val logger = LoggerFactory.getLogger(AndroidModuleGenerator::class.java)

    override val platform: Platform = Platform.ANDROID

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val template = toModuleTemplate(moduleName, config)
        return previewFromTemplate(template, projectRoot)
    }

    /**
     * Writes the module skeleton under [projectRoot].
     *
     * NOTE: [projectRoot] is the **Android Gradle root** (already resolved by the caller through
     * `workspaceRoot.resolve(config.path)`). Do NOT resolve [config.path] again here, otherwise
     * files land under `client/client/feature/<name>/` and the `:feature:<name>` entry that
     * [SettingsGradleUpdater] inserted in `client/settings.gradle.kts` will point at a missing
     * directory — Gradle then fails with "Configuring project ':feature:<name>' without an
     * existing directory".
     */
    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        val template = toModuleTemplate(moduleName, config)
        val created = mutableListOf<File>()

        for (entry in previewFromTemplate(template, projectRoot)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()

            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }

            created.add(file)
            logger.debug("Created: {}", file.absolutePath)
        }

        logger.info("Generated {} files for Android module '{}' under {}", created.size, moduleName, projectRoot.absolutePath)
        return created
    }

    /**
     * Registers the new `:feature:<name>` entry in the Android Gradle root's `settings.gradle.kts`.
     *
     * Matches the convention from [ModuleScaffolder.scaffoldForProject]: it passes the
     * **workspace root** as `projectRoot`, and the Android Gradle root lives at
     * `projectRoot.resolve(config.path)` (e.g. `android-test/client/`).
     */
    override fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        val subProjectRoot = projectRoot.resolve(config.path)
        settingsUpdater.update(subProjectRoot, moduleName)
    }

    fun previewFromTemplate(template: ModuleTemplate, projectRoot: File? = null): List<GeneratedFile> {
        val kotlinGen = KotlinModuleTemplateRenderer(templateEngine, template)
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/${template.name}"
        val isAndroid = template.isAndroid

        files.add(
            GeneratedFile(
                "$moduleDir/build.gradle.kts",
                kotlinGen.renderBuildGradle(projectRoot),
            ),
        )

        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathRootKoinModule()}", kotlinGen.renderRootKoinModule(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathDataModule()}", kotlinGen.renderDataModule(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathDomainModule()}", kotlinGen.renderDomainModule(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathPresentationModule()}", kotlinGen.renderPresentationModule(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathRepository()}", kotlinGen.renderRepositoryInterface(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathRepositoryImpl()}", kotlinGen.renderRepositoryImpl(projectRoot)))
        files.add(GeneratedFile("$moduleDir/${kotlinGen.pathViewModel()}", kotlinGen.renderViewModel(projectRoot)))

        if (isAndroid) {
            kotlinGen.renderContract(projectRoot)?.let {
                files.add(GeneratedFile("$moduleDir/${kotlinGen.pathContract()}", it))
            }
            kotlinGen.renderNavigationRoute(projectRoot)?.let {
                files.add(GeneratedFile("$moduleDir/${kotlinGen.pathNavigationRoute()}", it))
            }

            files.add(
                GeneratedFile(
                    "$moduleDir/src/main/AndroidManifest.xml",
                    kotlinGen.renderAndroidManifest(projectRoot),
                ),
            )
        }

        return files
    }

    fun generateFromTemplate(projectRoot: File, template: ModuleTemplate): List<File> {
        val created = mutableListOf<File>()

        for (entry in previewFromTemplate(template, projectRoot)) {
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

    companion object {
        fun toModuleTemplate(moduleName: String, config: SubProjectConfig): ModuleTemplate {
            val normalizedModule = moduleName.replace("-", "").replace("_", "")
            val featurePackage = "${config.basePackage}.feature.$normalizedModule"
            val isAndroid = config.platform in setOf(Platform.ANDROID, Platform.KMP_ANDROID)
            val namespace = if (isAndroid) featurePackage else null
            val conventionPluginId = resolveConventionPluginId(config, isAndroid)

            return ModuleTemplate(
                name = moduleName,
                packageName = featurePackage,
                conventionPluginId = conventionPluginId,
                layers = config.moduleStructure?.layers ?: listOf("data", "domain", "presentation"),
                hasRes = config.moduleStructure?.hasRes ?: false,
                namespace = namespace,
                projectType = config.platform.name,
                basePackage = config.basePackage,
                baseClassPackages = resolveBaseClasses(config),
            )
        }

        /**
         * Derives the Android convention plugin id for feature modules when
         * [SubProjectConfig.conventionPluginId] is not explicitly set.
         *
         * The default Android client template registers a convention plugin named
         * `<basePackage>.convention.feature`; this matches that naming so fresh scaffolds compile
         * against the template's `build-logic`.
         */
        private fun resolveConventionPluginId(config: SubProjectConfig, isAndroid: Boolean): String? {
            config.conventionPluginId?.takeIf { it.isNotBlank() }?.let { return it }
            if (!isAndroid) return null
            val base = config.basePackage.takeIf { it.isNotBlank() } ?: return null
            return "$base.convention.feature"
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
                pageResultClass = bp?.let { "$it.feature.base.domain.pagination.PageResult" },
                retrofitProvider = bp?.let { "$it.feature.common.network.DynamicRetrofitProvider" },
            )
        }
    }
}
