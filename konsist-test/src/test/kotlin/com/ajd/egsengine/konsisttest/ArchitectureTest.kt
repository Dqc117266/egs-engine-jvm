package com.ajd.egsengine.konsisttest

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {

    @Test
    fun `classes in data package should have 'Repository' or 'Runner' or 'Loader' or 'Impl' suffix`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.resideInPackage("..data..") }
            .assertTrue {
                it.name.endsWith("Repository") ||
                    it.name.endsWith("RepositoryImpl") ||
                    it.name.endsWith("Runner") ||
                    it.name.endsWith("Loader") ||
                    it.name.endsWith("Impl")
            }
    }

    @Test
    fun `classes in domain package should not depend on data package directly`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.resideInPackage("..domain..") }
            .assertTrue {
                !it.text.contains("import com.ajd.egsengine.feature.*.data")
            }
    }

    @Test
    fun `classes in presentation package should have 'Cli' suffix`() {
        Konsist
            .scopeFromProject()
            .classes()
            .filter { it.resideInPackage("..presentation..") }
            .withNameEndingWith("Cli")
            .assertTrue { it.name.endsWith("Cli") }
    }
}
