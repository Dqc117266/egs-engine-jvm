package com.dqc.egsengine.konsisttest

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {
    /**
     * Production source classes only — excludes golden snapshots, build output, test sources.
     */
    private fun productionClasses() = Konsist
        .scopeFromProject()
        .classes()
        .filter { !it.containingFile.path.contains("/build/") }
        .filter { !it.containingFile.path.contains("/src/test/") }
        .filter { !it.containingFile.path.contains("/golden/") }

    @Test
    fun `repository implementations should have Repository or Impl suffix`() {
        productionClasses()
            .filter { it.resideInPackage("..data.repository..") }
            .assertTrue {
                it.name.endsWith("Repository") || it.name.endsWith("Impl")
            }
    }

    @Test
    fun `classes in domain package should not depend on data package directly`() {
        productionClasses()
            .filter { it.resideInPackage("..domain..") }
            .assertTrue {
                !it.text.contains("import com.dqc.egsengine.feature.*.data")
            }
    }

    @Test
    fun `source files in presentation package follow naming convention`() {
        productionClasses()
            .filter { it.resideInPackage("..presentation..") }
            .assertTrue {
                val fileName = it.containingFile.name
                fileName.endsWith("Cli") ||
                    fileName.endsWith("Command") ||
                    fileName.endsWith("Resolver") ||
                    fileName.endsWith("Formatter") ||
                    fileName.endsWith("Error") ||
                    fileName.endsWith("Dto") ||
                    fileName.endsWith("Json")
            }
    }

    @Test
    fun `presentation layer in base and common should not import from data layer`() {
        productionClasses()
            .filter {
                it.resideInPackage("..feature.base.presentation..") ||
                    it.resideInPackage("..feature.common.presentation..")
            }
            .assertTrue {
                !it.text.contains("import com.dqc.egsengine.feature.*.data")
            }
    }
}
