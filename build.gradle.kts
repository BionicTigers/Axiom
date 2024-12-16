buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
        //1.8.10 = Latest Kotlin Version
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

repositories {
    mavenCentral()
}

group = "io.github.bionictigers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
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