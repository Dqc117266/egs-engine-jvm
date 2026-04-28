/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import java.io.File

/** Thin subclasses under non-`generate/` packages; created when missing unless [force]. */
class SpringBootHandWrittenShellGenerator {

    fun generateIfMissing(
        backendRoot: File,
        moduleName: String,
        config: SubProjectConfig,
        entityPascal: String,
        entityCamel: String,
        force: Boolean,
        dryRun: Boolean,
    ): List<GeneratedFile> {
        val featureSeg = moduleName.replace("-", "")
        val pkg = "${config.basePackage}.feature.$featureSeg"
        val kotlinRoot = "feature/$moduleName/src/main/kotlin/${pkg.replace('.', '/')}"
        val out = mutableListOf<GeneratedFile>()

        fun put(rel: String, content: String) {
            val path = "$kotlinRoot/$rel"
            val target = backendRoot.resolve(path)
            if (!dryRun && !force && target.exists()) return
            out.add(GeneratedFile(path = path, content = content.trimEnd() + "\n"))
        }

        put(
            "api/controller/${entityPascal}Controller.kt",
            """
            package $pkg.api.controller

            import $pkg.data.mapper.${entityPascal}EntityMapper
            import $pkg.generate.api.controller.Generated${entityPascal}Controller
            import $pkg.generate.domain.usecase.Count${entityPascal}UseCase
            import $pkg.generate.domain.usecase.Create${entityPascal}UseCase
            import $pkg.generate.domain.usecase.Delete${entityPascal}ByIdUseCase
            import $pkg.generate.domain.usecase.Get${entityPascal}AllUseCase
            import $pkg.generate.domain.usecase.Get${entityPascal}ByIdUseCase
            import $pkg.generate.domain.usecase.Get${entityPascal}PageUseCase
            import $pkg.generate.domain.usecase.Update${entityPascal}UseCase
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class ${entityPascal}Controller(
                create${entityPascal}: Create${entityPascal}UseCase,
                update${entityPascal}: Update${entityPascal}UseCase,
                delete${entityPascal}ById: Delete${entityPascal}ByIdUseCase,
                get${entityPascal}ById: Get${entityPascal}ByIdUseCase,
                get${entityPascal}Page: Get${entityPascal}PageUseCase,
                get${entityPascal}All: Get${entityPascal}AllUseCase,
                count${entityPascal}: Count${entityPascal}UseCase,
                mapper: ${entityPascal}EntityMapper,
            ) : Generated${entityPascal}Controller(
                create${entityPascal},
                update${entityPascal},
                delete${entityPascal}ById,
                get${entityPascal}ById,
                get${entityPascal}Page,
                get${entityPascal}All,
                count${entityPascal},
                mapper,
            )
            """.trimIndent(),
        )

        put(
            "data/repository/${entityPascal}RepositoryImpl.kt",
            """
            package $pkg.data.repository

            import $pkg.generate.data.datasource.jpa.${entityCamel}JpaRepository
            import $pkg.data.mapper.${entityPascal}EntityMapper
            import $pkg.generate.data.repository.Generated${entityPascal}DbRepositorySupport
            import $pkg.generate.domain.repository.${entityPascal}Repository
            import org.springframework.stereotype.Service

            @Service
            class ${entityPascal}RepositoryImpl(
                jpaRepository: ${entityCamel}JpaRepository,
                mapper: ${entityPascal}EntityMapper,
            ) : Generated${entityPascal}DbRepositorySupport(jpaRepository, mapper), ${entityPascal}Repository
            """.trimIndent(),
        )

        put(
            "data/mapper/${entityPascal}EntityMapper.kt",
            """
            package $pkg.data.mapper

            import $pkg.generate.data.mapper.Generated${entityPascal}EntityMapper
            import org.springframework.stereotype.Component

            @Component
            class ${entityPascal}EntityMapper : Generated${entityPascal}EntityMapper()
            """.trimIndent(),
        )

        put(
            "data/repository/${entityPascal}CacheRepositoryImpl.kt",
            """
            package $pkg.data.repository

            import $pkg.data.mapper.${entityPascal}CacheMapper
            import $pkg.generate.data.datasource.cache.${entityPascal}CacheDataSource
            import $pkg.generate.data.repository.Generated${entityPascal}CacheRepositorySupport
            import $pkg.generate.domain.repository.${entityPascal}CacheRepository
            import org.springframework.stereotype.Service

            @Service
            class ${entityPascal}CacheRepositoryImpl(
                cache: ${entityPascal}CacheDataSource,
                mapper: ${entityPascal}CacheMapper,
            ) : Generated${entityPascal}CacheRepositorySupport(cache, mapper), ${entityPascal}CacheRepository
            """.trimIndent(),
        )

        put(
            "data/repository/${entityPascal}HttpRepositoryImpl.kt",
            """
            package $pkg.data.repository

            import $pkg.data.mapper.${entityPascal}HttpMapper
            import $pkg.generate.data.datasource.httpclient.${entityPascal}HttpClient
            import $pkg.generate.data.repository.Generated${entityPascal}HttpRepositorySupport
            import $pkg.generate.domain.repository.${entityPascal}HttpRepository
            import org.springframework.stereotype.Service

            @Service
            class ${entityPascal}HttpRepositoryImpl(
                client: ${entityPascal}HttpClient,
                mapper: ${entityPascal}HttpMapper,
            ) : Generated${entityPascal}HttpRepositorySupport(client, mapper), ${entityPascal}HttpRepository
            """.trimIndent(),
        )

        put(
            "data/mapper/${entityPascal}CacheMapper.kt",
            """
            package $pkg.data.mapper

            import $pkg.generate.data.mapper.Generated${entityPascal}CacheMapper
            import org.springframework.stereotype.Component

            @Component
            class ${entityPascal}CacheMapper : Generated${entityPascal}CacheMapper()
            """.trimIndent(),
        )

        put(
            "data/mapper/${entityPascal}HttpMapper.kt",
            """
            package $pkg.data.mapper

            import $pkg.generate.data.mapper.Generated${entityPascal}HttpMapper
            import org.springframework.stereotype.Component

            @Component
            class ${entityPascal}HttpMapper : Generated${entityPascal}HttpMapper()
            """.trimIndent(),
        )

        return out
    }
}
