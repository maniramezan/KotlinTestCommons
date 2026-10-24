import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.maniramezan.testcommons.samples.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.maniramezan.testcommons.samples.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(project(":junit"))
    testImplementation(libs.kotlin.test)
    androidTestImplementation(project(":compose"))
    androidTestImplementation(project(":device"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlin.test)
}
