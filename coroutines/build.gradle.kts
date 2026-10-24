import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
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
        namespace = "io.github.maniramezan.testcommons.coroutines"
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

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.test)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

kover {
    reports {
        verify {
            rule {
                minBound(95)
            }
        }
    }
}
