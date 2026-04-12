plugins {
    id("com.dqc.egsengine.convention.feature")
}

dependencies {
    implementation(projects.feature.init)
    implementation(projects.feature.templateEngine)
}
