package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeProjectListener
import mu.KotlinLogging
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

private const val MONGO_3_6_IMAGE = "mongo:3.6"
private const val MONGO_6_0_IMAGE = "mongo:6.0"
private val CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(100)

class MongoExtension(
    private val mongo36dbName: String,
    private val mongo60dbName: String,
) : BeforeProjectListener, AfterProjectListener, AfterTestListener {
    private lateinit var mongo36client: MongoClient
    private lateinit var mongo60client: MongoClient
    private val logger = KotlinLogging.logger { }
    private val mongo36container = MongoDBContainer(MONGO_3_6_IMAGE)
    private val mongo60container = MongoDBContainer(MONGO_6_0_IMAGE)

    override suspend fun beforeProject() {
        logger.info { "Starting MongoDB containers: [$MONGO_3_6_IMAGE] and [$MONGO_6_0_IMAGE]" }
        startMongoContainersConcurrently()
        logger.info("MongoDB containers started: [$MONGO_3_6_IMAGE : ${mongo36uri()}], [$MONGO_6_0_IMAGE: ${mongo60uri()}]")
        mongo36client = MongoClients.create(mongo36uri())
        mongo60client = MongoClients.create(mongo60uri())
    }

    override suspend fun afterProject() {
        mongo36client.close()
        mongo60client.close()
        mongo36container.stop()
        mongo60container.stop()
    }

    fun mongo36uri(): String = mongo36container.getReplicaSetUrl(mongo36dbName)
    fun mongo60uri(): String = mongo60container.getReplicaSetUrl(mongo60dbName)
    fun mongo36db(): MongoDatabase = mongo36client.getDatabase(mongo36dbName)
    fun mongo60db(): MongoDatabase = mongo60client.getDatabase(mongo60dbName)

    private fun startMongoContainersConcurrently() {
        val time = measureTimeMillis {
            Stream.of(mongo36container, mongo60container)
                .parallel()
                .forEach { startAndWaitForMongoContainer(it) }
        }
        logger.warn { "Creating containers [$MONGO_3_6_IMAGE] and [$MONGO_6_0_IMAGE] took: [$time ms]" }
    }

    private fun startAndWaitForMongoContainer(mongoDBContainer: MongoDBContainer) {
        mongoDBContainer.start()
        mongoDBContainer.waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
    }

    private fun removeAllCollections() {
        removeAllCollectionsFromDb(mongo36db())
        removeAllCollectionsFromDb(mongo60db())
    }

    private fun removeAllCollectionsFromDb(db: MongoDatabase) {
        db.listCollectionNames().forEach {
            db.getCollection(it).drop()
        }
    }
}
