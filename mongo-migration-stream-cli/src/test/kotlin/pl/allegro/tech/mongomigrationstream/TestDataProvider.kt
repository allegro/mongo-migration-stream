package pl.allegro.tech.mongomigrationstream

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Updates
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.BsonDocument
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.properties.ApplicationPropertiesReader
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }
private const val SLEEP = 1000L
private const val AMOUNT_OF_INITIAL_DOCUMENTS = 0

private val applicationProperties = ApplicationPropertiesReader.readApplicationProperties(
    listOf("./config/local/application.properties")
)

fun main() {
    val sourceDbClient = mongoDbClient(applicationProperties.sourceDbProperties)
    val destinationDbClient = mongoDbClient(applicationProperties.destinationDbProperties)
    val executor = Executors.newFixedThreadPool(
        applicationProperties.sourceToDestinationMapping.size
    )

    applicationProperties.sourceToDestinationMapping
        .forEach { sourceToDestination ->
            executor.execute {
                startProvidingData(sourceDbClient, destinationDbClient, sourceToDestination)
            }
        }
}

private fun startProvidingData(
    sourceDbClient: MongoDatabase,
    destinationDbClient: MongoDatabase,
    sourceToDestination: SourceToDestination
) {
    val sourceCollection = sourceDbClient.getCollection(
        sourceToDestination.source.collectionName, BsonDocument::class.java
    )
    val destinationCollection =
        destinationDbClient.getCollection(sourceToDestination.destination.collectionName, BsonDocument::class.java)

    sourceCollection.deleteMany(Document())
    destinationCollection.drop()

    insertHugeAmountOfDataIntoSourceCollection(sourceCollection)
    startPerformanceInserts(sourceCollection)
}

private fun insertHugeAmountOfDataIntoSourceCollection(sourceCollection: MongoCollection<BsonDocument>) {
    if (AMOUNT_OF_INITIAL_DOCUMENTS != 0)
        sourceCollection.insertMany(
            List(AMOUNT_OF_INITIAL_DOCUMENTS) {
                Document().append("counter", it).toBsonDocument()
            }
        )
}

private fun startPerformanceInserts(sourceCollection: MongoCollection<BsonDocument>) {
    val globalCounter = AtomicInteger(AMOUNT_OF_INITIAL_DOCUMENTS + 1)
    val localCounter = AtomicInteger(AMOUNT_OF_INITIAL_DOCUMENTS + 1)

    sourceCollection.createIndex(Indexes.ascending("counter"), IndexOptions().name("counterIndex"))

    while (true) {
        val value = localCounter.getAndIncrement()
        operationWithDelay { insert(sourceCollection, value, globalCounter) }
        operationWithDelay { update(sourceCollection, value) }
        operationWithDelay { replace(sourceCollection, value) }
        operationWithDelay { delete(sourceCollection, value) }
    }
}

private fun operationWithDelay(operation: () -> Unit) {
    Thread.sleep(SLEEP)
    operation.invoke()
}

private fun insert(collection: MongoCollection<BsonDocument>, value: Int, counter: AtomicInteger) {
    logger.info { "Insert to collection: [${collection.namespace}], value: [$value]" }
    collection.insertMany(
        List(10) {
            Document()
                .append("_id", counter.getAndIncrement())
                .append("counter", value)
                .append("intTest", 1)
                .append("floatTest", 1.2f)
                .append("doubleTest", 1.2)
                .append("booleanTest", false)
                .append("nullTest", null)
                .append("charTest", 'c')
                .append("stringTest", "string")
                .append("dateTest", Date())
                .append("arrayTest", listOf(1, 2, 3))
                .toBsonDocument()
        }
    )
}

private fun update(collection: MongoCollection<BsonDocument>, value: Int) {
    val filter = Filters.eq("_id", value)
    logger.info { "Update to collection: [${collection.namespace}], value: [$value]" }
    collection.updateOne(
        filter,
        Updates.combine(
            Updates.set("counter", value + 1),
            Updates.set("updated", true),
            Updates.unset("doubleTest"),
            Updates.popFirst("arrayTest")
        ).toBsonDocument()
    )
}

private fun replace(collection: MongoCollection<BsonDocument>, value: Int) {
    val filter = Filters.eq("_id", value)
    logger.info { "Replace to collection: [${collection.namespace}], value: [$value]" }
    collection.replaceOne(
        filter,
        Document()
            .append("_id", value)
            .append("counter", value + 1)
            .append("replaced", true)
            .toBsonDocument()
    )
}

private fun delete(collection: MongoCollection<BsonDocument>, value: Int) {
    val filter = Filters.eq("_id", value)
    logger.info { "Delete to collection: [${collection.namespace}], value: [$value]" }
    collection.deleteOne(filter)
}

private fun mongoDbClient(
    mongoProperties: MongoProperties
): MongoDatabase = MongoClients.create(
    MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongoProperties.uri))
        .let {
            if (mongoProperties.authenticationProperties != null) it.credential(
                MongoCredential.createCredential(
                    mongoProperties.authenticationProperties!!.username,
                    mongoProperties.authenticationProperties!!.authDbName,
                    mongoProperties.authenticationProperties!!.password.toCharArray()
                )
            )
            else it
        }.build()
).getDatabase(mongoProperties.dbName)
