import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id(GradleShadowPlugin.plugin) version GradleShadowPlugin.version
}

dependencies {
    implementation(project(":mongo-migration-stream-core"))

    implementation(Logback.dependency)
    implementation(KotlinLogging.dependency)
    implementation(Properlty.dependency)
    implementation(Log4jOverSlf4j.dependency)
    implementation(Micrometer.dependency)
    implementation(MongoDbDriver.syncDependency)

    testImplementation(kotlin("test"))
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.getByName<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "pl.allegro.tech.mongomigrationstream.MainKt"
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
    }
}
