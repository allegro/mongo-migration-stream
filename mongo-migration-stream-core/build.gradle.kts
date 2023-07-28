import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    `maven-publish`
    signing
}

dependencies {
    implementation(MongoDbDriver.syncDependency)
    implementation(MongoDbDriver.reactiveDependency)
    implementation(KotlinLogging.dependency)
    implementation(Properlty.dependency)
    implementation(Failsafe.dependency)
    implementation(BigQueue.dependency) { exclude("log4j") }
    implementation(Log4jOverSlf4j.dependency)
    implementation(ApacheCommons.dependency)
    implementation(Micrometer.dependency)
    implementation(Guava.dependency)

    runtimeOnly(Snappy.dependency)
    runtimeOnly(Zstd.dependency)

    testImplementation(kotlin("test"))
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)

    testImplementation(platform(TestContainers.bom))
    testImplementation(TestContainers.testContainers)
    testImplementation(TestContainers.mongodb)

    testImplementation(Awaitility.dependency)
    testImplementation(Logback.dependency)
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/kotlin")
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
