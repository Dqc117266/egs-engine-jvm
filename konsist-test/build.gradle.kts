plugins {
    id("com.ajd.egsengine.convention.library")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
