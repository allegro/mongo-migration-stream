import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":mongo-migration-stream-core"))

    implementation(libs.logback)
    implementation(libs.properlty)
    implementation(libs.log4j)
    implementation(libs.micrometer)
    implementation(libs.mongo.sync)

    testImplementation(libs.kotlin.logging)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.kotest)
}

tasks.getByName<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "pl.allegro.tech.mongomigrationstream.MainKt"
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("mongo-migration-stream-cli")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}
