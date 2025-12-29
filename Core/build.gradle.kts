import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import java.util.Base64

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka") version "2.0.0"      // optional but recommended for javadoc
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

group = "io.github.bionictigers"
version = "0.2.3"

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

    defaultConfig {
        compileSdk = 35
        minSdk = 24
    }

    flavorDimensions += "environment"
    productFlavors {
        create("mock") { dimension = "environment" }
        create("prod") { dimension = "environment" }
    }

    // Configure test options
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                showStandardStreams = true
            }
        }
    }
}

val ftcVersion = "11.0.0"

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

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
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

val keyRaw = providers.gradleProperty("signingInMemoryKey").orNull
val pass = providers.gradleProperty("signingInMemoryKeyPassword").orNull?.ifBlank { null }

signing {
    // Feed the ASCII-armored PRIVATE key to Gradle’s signing
    if (!keyRaw.isNullOrBlank() && keyRaw.contains("BEGIN PGP PRIVATE KEY BLOCK")) {
        useInMemoryPgpKeys(keyRaw.replace("\r\n", "\n"), pass)
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    // Use your real artifactId here:
    coordinates("io.github.bionictigers.axiom", "core", version.toString())

    configure(
        AndroidSingleVariantLibrary(
            variant = "prodRelease",
            sourcesJar = true,
            publishJavadocJar = true
        )
    )

    pom {
        name.set("Axiom Core")
        description.set("FTC robotics Android/Kotlin core library.")
        url.set("https://github.com/bionictigers/axiom")
        licenses { license {
            name.set("Apache-2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }}
        developers { developer { id.set("bionictigers"); name.set("Bionic Tigers") } }
        scm {
            url.set("https://github.com/bionictigers/axiom")
            connection.set("scm:git:https://github.com/bionictigers/axiom.git")
            developerConnection.set("scm:git:ssh://git@github.com:bionictigers/axiom.git")
        }
    }
}