plugins {
    kotlin("jvm") version "1.9.23"
}

group = "io.github.bionictigers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    compileOnly("org.firstinspires.ftc:FtcCommon:9.0.1")
//    implementation("com.qualcomm.robotcore")

    implementation(project(":MockFTC"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}