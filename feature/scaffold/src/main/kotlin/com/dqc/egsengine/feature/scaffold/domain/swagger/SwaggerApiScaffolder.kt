package com.dqc.egsengine.feature.scaffold.domain.swagger

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

class SwaggerApiScaffolder(
    private val configReader: EgsConfigReader,
    private val swaggerParser: SwaggerParser,
    private val swaggerCodeGenerator: SwaggerCodeGenerator,
) {
    private val logger = LoggerFactory.getLogger(SwaggerApiScaffolder::class.java)

    fun scaffold(
        projectRoot: File,
        moduleName: String,
        swaggerLocation: String,
        customPackage: String? = null,
        dryRun: Boolean = false,
    ): Result {
        val config = configReader.read(projectRoot)
        val template = buildTemplate(config, moduleName, customPackage)
        val spec = swaggerParser.parse(swaggerLocation)
        val generated = swaggerCodeGenerator.generate(template, spec)

        if (dryRun) {
            return Result(files = generated.map { it.path }, dryRun = true)
        }

        generated.forEach { file ->
            val target = projectRoot.resolve(file.path)
            target.parentFile.mkdirs()
            file.content?.let { target.writeText(it) }
            logger.debug("Generated {}", file.path)
        }

        return Result(files = generated.map { it.path }, dryRun = false)
    }

    private fun buildTemplate(config: EgsConfig, moduleName: String, customPackage: String?): ModuleTemplate {
        val basePackage = customPackage ?: config.basePackage ?: "com.example"
        val normalizedModule = moduleName.replace("-", "").replace("_", "")
        val modulePackage = "$basePackage.feature.$normalizedModule"
        val namespace = if (config.projectType in listOf("ANDROID", "KMP_ANDROID")) modulePackage else null

        return ModuleTemplate(
            name = moduleName,
            packageName = modulePackage,
            conventionPluginId = config.conventionPluginId,
            layers = config.moduleStructure.layers,
            hasRes = config.moduleStructure.hasRes,
            namespace = namespace,
            projectType = config.projectType,
            basePackage = config.basePackage,
            baseClassPackages = resolveBaseClassPackages(config),
            apiResultClass = config.basePackage?.let { "$it.feature.base.data.retrofit.ApiResult" },
            commonResultClass = config.basePackage?.let { "$it.feature.base.data.retrofit.CommonResult" },
            toResultPackage = config.basePackage?.let { "$it.feature.base.data.retrofit" },
        )
    }

    private fun resolveBaseClassPackages(config: EgsConfig): BaseClassPackages {
        val baseClasses = config.baseClasses
        val bp = config.basePackage

        fun findBaseClass(name: String): String? =
            baseClasses.find { it.name == name }?.let { "${it.packageName}.$name" }

        return BaseClassPackages(
            baseViewModel = findBaseClass("BaseViewModel"),
            baseFragment = findBaseClass("BaseFragment"),
            resultClass = bp?.let { "$it.feature.base.domain.result.Result" },
            retrofitProvider = bp?.let { "$it.feature.common.network.DynamicRetrofitProvider" },
        )
    }

    data class Result(
        val files: List<String>,
        val dryRun: Boolean,
    )
}
