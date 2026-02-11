plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("signing")
}

group = "io.github.bionictigers.axiom"
version = "0.2.5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

val keyRaw = providers.gradleProperty("signingInMemoryKey").orNull
val pass = providers.gradleProperty("signingInMemoryKeyPassword").orNull?.ifBlank { null }

signing {
    if (!keyRaw.isNullOrBlank() && keyRaw.contains("BEGIN PGP PRIVATE KEY BLOCK")) {
        useInMemoryPgpKeys(keyRaw.replace("\r\n", "\n"), pass)
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.bionictigers.axiom", "annotations", version.toString())

    pom {
        name.set("Axiom Annotations")
        description.set("Runtime annotations shared by Axiom and Seek.")
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
