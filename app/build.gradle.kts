plugins {
    id("com.ajd.egsengine.convention.application")
}

application {
    mainClass.set("com.ajd.egsengine.AppKt")

    applicationDefaultJvmArgs = listOf(
        "-Xmx512m",
        "-Dfile.encoding=UTF-8",
    )
}

dependencies {
    implementation(projects.feature.base)
    implementation(projects.feature.common)
    implementation(projects.feature.command)
    implementation(projects.feature.task)
    implementation(projects.feature.script)
    implementation(projects.feature.analyzer)
    implementation(projects.feature.init)
    implementation(projects.feature.scaffold)
}
