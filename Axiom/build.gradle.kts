plugins {
    id("com.android.library") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.10"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

//    implementation("io.javalin:javalin:5.+")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation(kotlin("reflect"))

    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("com.squareup.moshi:moshi-adapters:1.14.0"
    )

    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1") //{
//        exclude(group = "org.nanohttpd", module = "nanohttpd")
//    }
}

fun DependencyHandler.mockImplementation(dependencyNotation: Any) {
    add("mockImplementation", dependencyNotation)
}

fun DependencyHandler.prodImplementation(dependencyNotation: Any) {
    add("prodImplementation", dependencyNotation)
}