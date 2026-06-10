package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Creates empty Spring Boot feature module skeletons:
 * - build.gradle.kts with convention plugin + Spring dependencies
 * - Empty layer packages (data/domain/presentation)
 * - Registered in settings.gradle.kts
 */
class SpringBootModuleGenerator(
    private val settingsUpdater: SettingsGradleUpdater,
    private val appDependencyUpdater: SpringBootAppDependencyUpdater,
) : PlatformModuleGenerator {
    private val logger = LoggerFactory.getLogger(SpringBootModuleGenerator::class.java)

    override val platform: Platform = Platform.SPRING_BOOT

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/$moduleName"
        val basePackage = config.basePackage
        val pkgPath = "$basePackage.feature.$moduleName".replace('.', '/')

        files.add(GeneratedFile("$moduleDir/build.gradle.kts", generateBuildFile(config)))

        // Aligns with egs-server-template `feature/demo` (generate/ + three data sources + thin shells).
        val layers =
            listOf(
                "api/controller",
                "config",
                "data/mapper",
                "data/repository",
                "generate/api/controller",
                "generate/api/dto",
                "generate/config",
                "generate/data/datasource/cache",
                "generate/data/datasource/cache/key",
                "generate/data/datasource/cache/model",
                "generate/data/datasource/jpa",
                "generate/data/datasource/jpa/entity",
                "generate/data/datasource/httpclient",
                "generate/data/datasource/httpclient/model",
                "generate/data/mapper",
                "generate/data/repository",
                "generate/domain/model",
                "generate/domain/repository",
                "generate/domain/usecase",
            )
        for (layer in layers) {
            files.add(GeneratedFile("$moduleDir/src/main/kotlin/$pkgPath/$layer/.gitkeep", ""))
        }
        files.add(GeneratedFile("$moduleDir/GENERATOR.md", placeholderGeneratorReadme(moduleName)))

        return files
    }

    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        // projectRoot is already the sub-project root (resolved by caller in scaffoldForProject)
        val created = mutableListOf<File>()

        for (entry in preview(projectRoot, moduleName, config)) {
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

        logger.info("Generated {} files for Spring Boot module '{}'", created.size, moduleName)
        return created
    }

    override fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        val subProjectRoot = projectRoot.resolve(config.path)
        settingsUpdater.update(subProjectRoot, moduleName)
        appDependencyUpdater.ensureFeatureDependency(subProjectRoot, moduleName)
    }

    private fun generateBuildFile(config: SubProjectConfig): String {
        val conventionPluginId = config.conventionPluginId ?: "com.egs.server.convention.feature"
        val isEgsServer = conventionPluginId.contains("egs.server")
        return buildString {
            appendLine("plugins {")
            appendLine("    id(\"$conventionPluginId\")")
            appendLine("}")
            appendLine()
            appendLine("group = \"${config.basePackage}\"")
            appendLine("version = rootProject.version")
            appendLine()
            if (isEgsServer) {
                appendLine("dependencies {")
                appendLine("    // Cache + HTTP client layers (mirrors KMP `preferences` + `api` datasources).")
                appendLine("    implementation(libs.spring.boot.starter.data.redis)")
                appendLine("    implementation(libs.spring.boot.starter.web)")
                appendLine("    implementation(libs.spring.boot.starter.security)")
                appendLine("    implementation(libs.spring.boot.starter.aop)")
                appendLine("}")
            } else {
                appendLine("dependencies {")
                appendLine("    // Spring Boot web is required for @RestController / @Controller")
                appendLine("    implementation(libs.spring.boot.starter.web)")
                appendLine("    implementation(libs.spring.boot.starter.security)")
                appendLine("    implementation(libs.spring.boot.starter.aop)")
                appendLine("}")
            }
        }
    }

    private fun placeholderGeneratorReadme(moduleName: String): String =
        """
        # feature:$moduleName

        Empty module skeleton aligned with `egs-server-template/feature/demo` package layout.

        ## Next step: fill CRUD from SQL

        From the **backend** repo root:

        ```bash
        egs-engine backend gen database path/to/table.sql --module=$moduleName
        ```

        That command **overwrites** everything under `generate/` (domain model/entity, JPA + cache + HTTP
        datasources, repository interfaces + `Generated*Support` + mappers, seven use cases, DTOs,
        `Generated*Controller`, `Generated*Config`) and writes `.egs-generated.json` so the next run can
        delete stale generated files first.

        It also creates **hand-written shells** when missing (`api/controller/*Controller.kt`,
        `data/repository/*RepositoryImpl.kt`, cache/HTTP repository impls, `data/mapper/*Mapper.kt`,
        `config/*Properties.kt`, `config/*FeatureConfig.kt`). Existing shells are left alone unless you pass
        `--force`.

        In the default **opinionated** mode it matches the demo stack (status enum, audit columns,
        soft-delete, optimistic lock, `@PreAuthorize`, Flyway `V*__feature_*_audit_fields.sql` when columns
        are missing). Use `--no-audit`, `--no-soft-delete`, or `--no-status-enum` to turn pieces off.

        If `feature/$moduleName` does not exist yet, `gen database` will run `backend module create` for you.
        """.trimIndent()
}
