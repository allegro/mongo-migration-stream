# mongo-migration-stream

_mongo-migration-stream_ is a tool for online migrations of MongoDB databases.
It transfers all the data from source database to the destination database, and keeps eventual consistency between those
two MongoDB databases - allowing you to switch from one database to another without any downtime.
To perform migrations, _mongo-migration-stream_ utilises:

- [MongoDB Database Tools](https://www.mongodb.com/docs/database-tools/) - to dump and restore the data,
- [Mongo Change Events](https://www.mongodb.com/docs/manual/reference/change-events/) - to keep synchronization between source and destination database,
- A Kotlin application - to manage, orchestrate and monitor all underneath processes.

## Requirements

- Installed [MongoDB Database Tools](https://www.mongodb.com/docs/database-tools/#installation) on the machine,
- Source MongoDB instance is either [replica set](https://www.mongodb.com/docs/manual/replication/) or [sharded cluster](https://www.mongodb.com/docs/manual/sharding/),
- Source MongoDB instance is in version >= _3.6_.

## Usage

_mongo-migration-stream_ can be used in two ways:

- as a dependency in JVM application,
- as a standalone JAR from command line.

### As dependency

Firstly, add _mongo-migration-stream_ as a dependency to your project:

```gradle
dependencies {
    implementation("pl.allegro.tech:mongo-migration-stream:0.9.0")
}
```

Then, you can perform migrations using `MongoMigrationStream` object:

```kotlin
val migrator = MongoMigrationStream(
  ApplicationProperties(
    generalProperties = GeneralProperties(
      shouldPerformTransfer = shouldPerformTransfer,
      shouldPerformSynchronization = shouldPerformSynchronization,
      synchronizationHandlers = synchronizationHandlers,
      synchronizationDetectors = synchronizationDetectors,
      databaseValidators = startMongoMigrationStreamInfo.validators
    ),
    sourceDbProperties = startMongoMigrationStreamInfo.source,
    destinationDbProperties = startMongoMigrationStreamInfo.destination,
    collectionsProperties = startMongoMigrationStreamInfo.collections,
    performerProperties = PerformerProperties(
      rootPath,
      configuration.mongoToolsPath,
      configuration.queueFactoryType,
      startMongoMigrationStreamInfo.dumpReadPreference,
      batchSizeProvider,
      false,
      1
    ),
    stateConfig = StateConfig(stateEventHandler)
  ),
  meterRegistry
)

migrator.start()    // Starts a migration
migrator.pause()    // Pauses a migration
migrator.resume()   // Resumes a migration from a pause
migrator.stop()     // Stops a migration
```

### As standalone JAR

Firstly, you need a _mongo-migration-stream_ JAR. You can either:

- Download it from the [newest release page](https://github.com/allegro/mongo-migration-stream/releases), or
- Clone project repository and execute `./gradlew shadowJar` command in root project directory - a JAR file should
  appear in `mongo-migration-stream-cli/build/libs/` directory.

Secondly, you have to define a properties file specifying migration details.

Below you can find a simple example of `application.properties` file:

```properties
perform.transfer=true
perform.synchronization=true
perform.synchronization.handlers=LoggingSynchronizationHandler
perform.synchronization.detectors=QueueSize,CollectionCount,DbHash
perform.synchronization.validators=DbAvailability,DestinationCollectionMissing,SourceCollectionAvailable

custom.rootPath=/tmp/mongomigrationstream/
custom.queue.factory=BiqQueueFactory
custom.dumpReadPreference=primary
custom.isCompressionEnabled=false
custom.insertionWorkersPerCollection=1
custom.batchSize=1000

collections.source=collection1,collection2
collections.destination=collection1changedName,collection2changedName

# Source - Replica set 3.6
source.db.uri=mongodb://mongo_rs36_1:36301,mongo_rs36_2:36302,mongo_rs36_3:36303/?replicaSet=replicaSet36
source.db.name=test
source.db.authentication.enabled=false

# Destination - Replica set 5.0
destination.db.uri=mongodb://mongo_rs5_1:50301,mongo_rs5_2:50302,mongo_rs5_3:50303/?replicaSet=replicaSet5
destination.db.name=test
destination.db.authentication.enabled=false
```

Having both JAR and properties file, you can start the migration with a command:

```
java -jar mongo-migration-stream-cli.jar --config application.properties
```

## More about _mongo-migration-stream_ project

You can find more information about _mongo-migration-stream_ in:
- [Allegro Tech Blog post about the reasons why we've created mongo-migration-stream and how it works](https://blog.allegro.tech/2023/09/online-mongodb-migration.html),
- TODO: Documentation
