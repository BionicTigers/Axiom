pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

include(":MockFTC")
include(":Axiom")
project(":MockFTC").projectDir = file("MockFTC")
project(":Axiom").projectDir = file("Axiom")

rootProject.name = "axiom"