object MongoMigrationStream {
    const val group = "pl.allegro.tech"
}

object Jvm {
    const val version = "11"
}

object Kotlin {
    const val version = "1.8.10"
    const val plugin = "kotlin"
}

object KtlintPlugin {
    const val version = "10.3.0"
    const val plugin = "org.jlleitschuh.gradle.ktlint"
}

object DetektPlugin {
    const val version = "1.21.0"
    const val plugin = "io.gitlab.arturbosch.detekt"
}

object AxionReleasePlugin {
    const val version = "1.14.3"
    const val plugin = "pl.allegro.tech.build.axion-release"
}

object GradleShadowPlugin {
    const val version = "8.1.1"
    const val plugin = "com.github.johnrengelman.shadow"
}

object NexusPublishPlugin {
    const val version = "1.3.0"
    const val plugin = "io.github.gradle-nexus.publish-plugin"
}

object MongoDbDriver {
    private const val version = "4.8.2"
    const val syncDependency = "org.mongodb:mongodb-driver-sync:$version"
    const val reactiveDependency = "org.mongodb:mongodb-driver-reactivestreams:$version"
}

object Snappy {
    private const val version = "1.1.9.1"
    const val dependency = "org.xerial.snappy:snappy-java:$version"
}

object Zstd {
    private const val version = "1.5.4-1"
    const val dependency = "com.github.luben:zstd-jni:$version"
}

object Logback {
    private const val version = "1.2.11"
    const val dependency = "ch.qos.logback:logback-classic:$version"
}

object KotlinLogging {
    private const val version = "2.1.23"
    const val dependency = "io.github.microutils:kotlin-logging-jvm:$version"
}

object Properlty {
    private const val version = "1.8.1"
    const val dependency = "com.ufoscout.properlty:properlty-kotlin:$version"
}

object Failsafe {
    private const val version = "3.2.4"
    const val dependency = "dev.failsafe:failsafe:$version"
}

object Kotest {
    private const val version = "5.3.1"
    const val runnerJunit5 = "io.kotest:kotest-runner-junit5:$version"
    const val assertionsCore = "io.kotest:kotest-assertions-core:$version"
}

object TestContainers {
    private const val version = "1.17.5"
    const val bom = "org.testcontainers:testcontainers-bom:$version"
    const val testContainers = "org.testcontainers:testcontainers"
    const val mongodb = "org.testcontainers:mongodb"
}

object Awaitility {
    private const val version = "4.2.0"
    const val dependency = "org.awaitility:awaitility-kotlin:$version"
}

object BigQueue {
    private const val version = "0.7.0"
    const val repositoryUri = "https://raw.github.com/bulldog2011/bulldog-repo/master/repo/releases/"
    const val dependency = "com.leansoft:bigqueue:$version"
}

object Log4jOverSlf4j {
    private const val version = "1.7.36"
    const val dependency = "org.slf4j:log4j-over-slf4j:$version"
}

object ApacheCommons {
    private const val version = "3.12.0"
    const val dependency = "org.apache.commons:commons-lang3:$version"
}

object Micrometer {
    private const val version = "1.9.3"
    const val dependency = "io.micrometer:micrometer-registry-jmx:$version"
}

object Guava {
    private const val version = "31.1-jre"
    const val dependency = "com.google.guava:guava:$version"
}
