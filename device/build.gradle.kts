import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    // Android-only, but built as a KMP library so KGP's ABI validation covers it like the other modules.
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    android {
        namespace = "io.github.maniramezan.testcommons.device"
        compileSdk = 37
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":core"))
            api(libs.androidx.test.core)
            api(libs.androidx.test.runner)
            api(libs.androidx.test.espresso.core)
            api(libs.androidx.test.uiautomator)
            api(libs.junit4)
        }
        named("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.robolectric)
        }
        named("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}

kover {
    reports {
        verify {
            rule {
                // UiAutomator and Espresso code runs only on a device; the floor covers the
                // pure logic extracted into internal functions. Floors only go up.
                minBound(30)
            }
        }
    }
}
