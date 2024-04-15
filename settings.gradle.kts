plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
include(":MockFTC")
project(":MockFTC").projectDir = file("MockFTC")
rootProject.name = "axiom"

