plugins {
    // Convention plugins
    id("com.dqc.egsengine.convention.detekt")
    id("com.dqc.egsengine.convention.spotless")

    // Core Kotlin plugins using version catalog
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.test.logger) apply false
}
