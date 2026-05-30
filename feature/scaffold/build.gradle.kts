plugins {
    id("com.dqc.egsengine.convention.feature")
}

dependencies {
    implementation(projects.feature.init)
    implementation(projects.feature.templateEngine)
    implementation(libs.kotlinpoet)
}

// Forward golden-snapshot control flags to the forked test JVM. The convention plugin
// resets `systemProperties`, so these would otherwise be dropped.
//   ./gradlew :feature:scaffold:test -Degs.golden.update=true   # refresh golden corpus
//   ./gradlew :feature:scaffold:test -Degs.golden.dir=/abs/dir  # override golden root
tasks.withType<Test>().configureEach {
    listOf("egs.golden.update", "egs.golden.dir").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}
