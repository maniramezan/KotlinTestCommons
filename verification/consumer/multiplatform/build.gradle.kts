import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

val testCommonsVersion = providers.gradleProperty("testCommonsVersion").get()

kotlin {
    explicitApi()
    jvm {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }
    android {
        namespace = "io.github.maniramezan.testcommons.verification"
        compileSdk = 37
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()
    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("androidx.compose.ui.test.ExperimentalTestApi")
    }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.maniramezan.testcommons:core:$testCommonsVersion")
            implementation("io.github.maniramezan.testcommons:compose:$testCommonsVersion")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation("io.github.maniramezan.testcommons:device:$testCommonsVersion")
        }
        named("androidHostTest").dependencies {
            implementation("io.github.maniramezan.testcommons:junit:$testCommonsVersion")
        }
    }
}
