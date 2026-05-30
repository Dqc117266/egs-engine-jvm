plugins {
    id("com.dqc.egsengine.convention.library")
}

dependencies {
    implementation(libs.freemarker)
    implementation(libs.koin.core)

    testImplementation(libs.bundles.test)
}
