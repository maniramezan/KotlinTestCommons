plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

// Single source of truth for the published version: the release workflow injects VERSION_NAME
// from the computed SemVer tag. Local and CI builds without a release tag use a snapshot version.
val libraryVersion: String =
    providers.gradleProperty("VERSION_NAME").orElse("0.0.0-SNAPSHOT").get()

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = libraryVersion
}

val ktlintVersion = libs.versions.ktlint.get()

// settings.gradle.kts declares the Node.js, Yarn, and Binaryen download repositories, so the
// Kotlin/JS and Kotlin/Wasm plugins must not add their own project repositories.
allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}

val stageRelease by tasks.registering {
    group = "verification"
    description = "Stages every published variant in a file Maven repository for independent consumer verification."
}

subprojects {
    val publishedProjectPath = path
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories.maven {
                name = "ReleaseVerification"
                url = rootProject.layout.buildDirectory.dir("release-staging").get().asFile.toURI()
            }
        }
        stageRelease.configure {
            dependsOn("$publishedProjectPath:publishAllPublicationsToReleaseVerificationRepository")
        }
    }
}

// House style lives in .editorconfig; `./gradlew ktlintCheck` enforces it and `ktlintFormat` fixes it.
subprojects {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version = ktlintVersion
    }
}

// Every published module's API pages link to their source on GitHub and open with Module.md.
val repositoryRoot = rootProject.layout.projectDirectory.asFile
subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                includes.from(layout.projectDirectory.file("Module.md"))
                sourceLink {
                    localDirectory.set(repositoryRoot)
                    remoteUrl("https://github.com/maniramezan/KotlinTestCommons/tree/main")
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

// Aggregated API docs (`./gradlew dokkaGenerate` -> build/dokka/html) and merged coverage
// (`./gradlew koverXmlReport` -> build/reports/kover/report.xml) for every published module.
dependencies {
    dokka(project(":core"))
    dokka(project(":junit"))
    dokka(project(":coroutines"))
    dokka(project(":compose"))
    dokka(project(":device"))
    kover(project(":core"))
    kover(project(":junit"))
    kover(project(":coroutines"))
    kover(project(":compose"))
    kover(project(":device"))
}

// Updates every module's checked-in public API dump after an intended API change.
tasks.register("updateApi") {
    group = "verification"
    description = "Updates the public API dumps of all published modules."
    dependsOn(":core:updateKotlinAbi", ":junit:updateKotlinAbi", ":coroutines:updateKotlinAbi", ":compose:updateKotlinAbi", ":device:updateKotlinAbi")
}
