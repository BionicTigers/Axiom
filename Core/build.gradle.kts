plugins {
//    id("com.android.application")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "io.github.bionictigers"
    compileSdk = 34

    flavorDimensions += "environment"

    productFlavors {
        create("mock") {
            dimension = "environment"
        }
        create("prod") {
            dimension = "environment"
        }
    }

    // Configure test options
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

val ftcVersion = "10.2.0"

dependencies {
    mockImplementation(project(":MockFTC"))
    mockImplementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    prodImplementation("org.firstinspires.ftc:Blocks:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:RobotCore:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:RobotServer:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:OnBotJava:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:Hardware:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:FtcCommon:$ftcVersion")
    prodImplementation("org.firstinspires.ftc:Vision:$ftcVersion")
    add("prodImplementation", "org.nanohttpd:nanohttpd-websocket:2.3.1") {
        exclude(group = "org.nanohttpd", module = "nanohttpd")
    }

    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("com.squareup.moshi:moshi-adapters:1.14.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation(kotlin("reflect"))
}

private fun DependencyHandlerScope.mockImplementation(dependency: ProjectDependency) {
    add("mockImplementation", dependency)
}

private fun DependencyHandlerScope.mockImplementation(dependency: String) {
    add("mockImplementation", dependency)
}

private fun DependencyHandlerScope.prodImplementation(dependency: String) {
    add("prodImplementation", dependency)
}
