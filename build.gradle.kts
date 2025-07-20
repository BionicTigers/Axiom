plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.10"
    id("com.android.library") version "8.5.2" apply false
}

android {
    namespace = "io.github.bionictigers"
    compileSdk = 33

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

dependencies {
    mockImplementation(project(":MockFTC"))

    prodImplementation("org.firstinspires.ftc:Blocks:10.1.1")
    prodImplementation("org.firstinspires.ftc:RobotCore:10.1.1")
    prodImplementation("org.firstinspires.ftc:RobotServer:10.1.1")
    prodImplementation("org.firstinspires.ftc:OnBotJava:10.1.1")
    prodImplementation("org.firstinspires.ftc:Hardware:10.1.1")
    prodImplementation("org.firstinspires.ftc:FtcCommon:10.1.1")
    prodImplementation("org.firstinspires.ftc:Vision:10.1.1")

    testImplementation(kotlin("test"))
}

private fun DependencyHandlerScope.mockImplementation(dependency: ProjectDependency) {
    add("mockImplementation", dependency)
}

private fun DependencyHandlerScope.prodImplementation(dependency: String) {
    add("prodImplementation", dependency)
}
