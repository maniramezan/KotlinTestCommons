import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    android {
        namespace = "io.github.maniramezan.testcommons.compose"
        compileSdk = 37
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_17
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    jvm {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }
    iosArm64()
    iosSimulatorArm64()

    // `jvmShared` holds the JUnit 4 `ComposeTestRule` helpers, available on Android and desktop.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("androidx.compose.ui.test.ExperimentalTestApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            api(libs.compose.ui.test)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
        named("jvmSharedMain").dependencies {
            api(libs.compose.ui.test.junit4)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.foundation)
        }
        jvmTest.dependencies {
            // Desktop UI tests need a Skiko renderer for the host OS.
            implementation(compose.desktop.currentOs)
        }
        named("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.activity.compose)
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

tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    // Robolectric needs hardware pixel copies for captureToImage(), and JDK internals opened on 17+.
    systemProperty("robolectric.pixelCopyRenderMode", "hardware")
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
    )
}
