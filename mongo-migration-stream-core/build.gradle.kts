plugins {
    `maven-publish`
    signing
}

dependencies {
    implementation(libs.bundles.mongo.drivers)
    implementation(libs.kotlin.logging)
    implementation(libs.properlty)
    implementation(libs.failsafe)
    implementation(libs.bigqueue) { exclude("log4j") }
    implementation(libs.log4j)
    implementation(libs.apache.commons)
    implementation(libs.micrometer)
    implementation(libs.guava)

    runtimeOnly(libs.bundles.compressors)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.kotest)

    testImplementation(libs.bundles.testcontainers)

    testImplementation(libs.awaitility)
    testImplementation(libs.logback)
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    create("integrationTest") {
        kotlin {
            srcDir("src/integrationTest/kotlin")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
}

tasks.named<ProcessResources>("processIntegrationTestResources").configure {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "mongo-migration-stream"
            from(components["java"])

            pom {
                name.set("mongo-migration-stream")
                description.set("Tool for online migrations of MongoDB databases")
                url.set("https://github.com/allegro/mongo-migration-stream/")
                inceptionYear.set("2022")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("pitagoras3")
                        name.set("Szymon Marcinkiewicz")
                    }
                }
                scm {
                    url.set("https://github.com/allegro/mongo-migration-stream")
                    connection.set("scm:git@github.com:allegro/mongo-migration-stream.git")
                    developerConnection.set("scm:git@github.com:allegro/mongo-migration-stream.git")
                }
            }
        }
    }
}

System.getenv("GPG_KEY_ID")?.let { gpgKeyId ->
    signing {
        useInMemoryPgpKeys(
            gpgKeyId,
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PRIVATE_KEY_PASSWORD")
        )
        sign(publishing.publications)
    }
}
