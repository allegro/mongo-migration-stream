import io.gitlab.arturbosch.detekt.DetektPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.axion.release)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.nexus)
}

scmVersion {
    tag {
        prefix.set(project.rootProject.name + "-")
    }
    versionCreator("versionWithBranch")
}

allprojects {
    apply(plugin = "kotlin")

    kotlin {
        jvmToolchain(11)
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://raw.github.com/bulldog2011/bulldog-repo/master/repo/releases/") }
    }
}

subprojects {
    apply<DetektPlugin>()
    apply<KtlintPlugin>()

    project.group = "pl.allegro.tech"
    project.version = rootProject.scmVersion.version

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }

    detekt {
        ignoreFailures = true
    }
}

group = "pl.allegro.tech"
version = scmVersion.version

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}
