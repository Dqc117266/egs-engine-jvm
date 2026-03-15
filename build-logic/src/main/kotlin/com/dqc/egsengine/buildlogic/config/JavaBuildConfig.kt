package com.dqc.egsengine.buildlogic.config

import org.gradle.api.JavaVersion
import java.io.File

object JavaBuildConfig {
    /**
     * Reads the Java version from the `gradle/libs.versions.toml` file.
     * (VersionCatalogsExtension is not available at this stage).
     */
    private val tomlJavaVersion by lazy {
        findVersionCatalog()
            .readLines()
            .firstOrNull { it.trim().startsWith("java") }
            ?.substringAfter("=")
            ?.trim('"', ' ')
            ?: error("Could not find 'java' version in libs.versions.toml file")
    }

    private fun findVersionCatalog(): File {
        var dir = File(System.getProperty("user.dir"))
        while (dir.parentFile != null) {
            val candidate = dir.resolve("gradle/libs.versions.toml")
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        error("Could not find gradle/libs.versions.toml in any parent directory")
    }

    val JAVA_VERSION: JavaVersion = JavaVersion.toVersion(tomlJavaVersion)
    val JVM_TOOLCHAIN_VERSION: Int = tomlJavaVersion.toInt()
}
