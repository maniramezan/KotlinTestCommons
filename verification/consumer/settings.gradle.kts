pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "TestCommonsUnderVerification"
                    url = providers.gradleProperty("testCommonsRepository")
                        .map { uri(it) }
                        .getOrElse(uri("https://repo.maven.apache.org/maven2"))
                }
            }
            filter {
                includeGroup("io.github.maniramezan.testcommons")
            }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "TestCommonsConsumerVerification"
include(":multiplatform", ":runtime", ":junit4", ":jupiter")
