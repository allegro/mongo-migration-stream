import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version apply false
    id(AxionReleasePlugin.plugin) version AxionReleasePlugin.version
    id(DetektPlugin.plugin) version DetektPlugin.version
    id(KtlintPlugin.plugin) version KtlintPlugin.version
    id(NexusPublishPlugin.plugin) version NexusPublishPlugin.version
}

scmVersion {
    tag {
        prefix.set(project.rootProject.name + "-")
    }
    versionCreator("versionWithBranch")
}

allprojects {
    apply(plugin = Kotlin.plugin)

    repositories {
        mavenCentral()
        maven { url = uri(BigQueue.repositoryUri) }
    }
}

subprojects {
    apply(plugin = DetektPlugin.plugin)
    apply(plugin = KtlintPlugin.plugin)

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    detekt {
        toolVersion = DetektPlugin.version
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
