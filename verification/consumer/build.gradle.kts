plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.ktlint)
}

val testCommonsVersion = providers.gradleProperty("testCommonsVersion")
    .orElse(providers.provider { error("Pass -PtestCommonsVersion=<exact version> to verify a release") })
    .get()
require(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[0-9A-Za-z.-]+)?").matches(testCommonsVersion)) {
    "testCommonsVersion must be an exact version"
}

val ktlintVersion = libs.versions.ktlint.get()

allprojects {
    group = "io.github.maniramezan.testcommons.verification"
    version = testCommonsVersion
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version = ktlintVersion
    }
}

tasks.register("verifyConsumer") {
    group = "verification"
    description = "Verifies Maven consumers on JVM and Android, and compiles both iOS variants on macOS."
    dependsOn(
        ":multiplatform:jvmTest",
        ":multiplatform:testAndroidHostTest",
        ":multiplatform:compileAndroidMain",
        ":multiplatform:compileKotlinIosArm64",
        ":multiplatform:compileKotlinIosSimulatorArm64",
        ":junit4:test",
        ":jupiter:test",
        ":runtime:run",
        "ktlintCheck",
    )
    subprojects.forEach { dependsOn("${it.path}:ktlintCheck") }
}
