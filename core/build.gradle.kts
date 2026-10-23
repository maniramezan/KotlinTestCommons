import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()

    // `checkKotlinAbi` (part of `check`) fails when the public surface changes without
    // `./gradlew :core:updateKotlinAbi` updating core/api/.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    android {
        namespace = "io.github.maniramezan.testcommons"
        compileSdk = 37
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        withHostTest {}
    }
    jvm {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }
    iosArm64()
    iosSimulatorArm64()

    // `jvmShared` holds the java.io / java.nio helpers shared by the JVM and Android targets.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                // The Android-KMP library target is not matched by `withAndroidTarget()`.
                withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        matching { it.name.endsWith("Test") }.configureEach {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
        commonMain.dependencies {
            // The core stays free of testing frameworks so tools can ship it.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

kover {
    reports {
        verify {
            rule {
                minBound(98)
            }
        }
    }
}
