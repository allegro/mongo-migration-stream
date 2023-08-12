# Configuration

_mongo-migration-stream_ can be used as a library in JVM application, or as a separate process
started from CLI.

## Library configuration

To configure _mongo-migration-stream_ library, you have to use `pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties` class.

``` kotlin
ApplicationProperties(
    generalProperties = GeneralProperties(
        shouldPerformTransfer = true, // Should copy all existing data from source to destination
        shouldPerformSynchronization = true, // Should synchronize new changes from source to destination
        synchronizationHandlers = setOf(LoggingDetectionResultHandler), // Set of handlers which are run periodically during synchronization state detection
        synchronizationDetectors = setOf( // Collection equality verification performed periodically on each source and destination collection
            DbHashSynchronizationDetectorType,
            QueueSizeSynchronizationDetectorType,
            CollectionCountSynchronizationDetectorType
        ),
        databaseValidators = setOf( // Validators used before starting migration
            DbAvailabilityValidatorType,
            DestinationMissingCollectionType,
            SourceCollectionAvailabilityType
        )
    ),
    sourceDbProperties = MongoProperties( // Source database properties
        uri = "mongodb://mongo_rs36_1:36301,mongo_rs36_2:36302,mongo_rs36_3:36303/?replicaSet=replicaSet36", // URI of source database
        dbName = "test", // Name of source MongoDB database
        authenticationProperties = MongoAuthenticationProperties( // Optional authentication properties for source database
            username = "username", // Username for source database
            password = "password", // Password for source database
            authDbName = "admin" // Source authentication database name
        )
    ),
    destinationDbProperties = MongoProperties( // Destination database properties
        uri = "mongodb://mongo_rs5_1:50301,mongo_rs5_2:50302,mongo_rs5_3:50303/?replicaSet=replicaSet5", // URI of destination database
        dbName = "test", // Name of destination MongoDB database
        authenticationProperties = MongoAuthenticationProperties( // Optional authentication properties for destination database
            username = "username", // Username for destination database
            password = "password", // Password for destination database
            authDbName = "admin" // Destination authentication database name
        )
    ),
    collectionsProperties = CollectionsProperties(
        sourceCollections = listOf("collection1", "collection2"), // Ordered list of collection names from source database which should be migrated
        destinationCollections = listOf("collection1changedName", "collection2changedName") // Ordered list of collection names where data from source collections should be migrated
    ),
    performerProperties = PerformerProperties(
        rootPath = "/tmp/mongomigrationstream/", // Path to a directory where all mongo-migration-stream files will be saved
        mongoToolsPath = "/usr/local/bin/", // Path to a directory where mongodump and mongorestore binaries are stored
        queueFactory = BiqQueueFactoryType, // What kind of queue should be used for storing synchronization events
        dumpReadPreference = ReadPreference.primary(), // Read preference used for mongodump
        batchSizeProvider = ConstantValueBatchSizeProvider(1_000) // Maximum size of a batch which is send from mongo-migration-stream to the destination
    ),
    stateConfig = StateConfig(
        stateEventHandler = LoggingStateEventHandler // Defines how application should handle changes of migration states with specific event handler
    )
)
```

## CLI configuration

To configure _mongo-migration-stream_ CLI, you need to specify file with configuration properties.
Below you can find all properties available:

``` properties
# [optional, default=true] 
# Should copy all existing data from source to destination
perform.transfer=true

# [optional, default=true] 
# Should synchronize new changes from source to destination
perform.synchronization=true

# [optional, default=[], availableOptions=[LoggingSynchronizationHandler]] 
# List of handlers (splitted with comma) which are run periodically during synchronization state detection
perform.synchronization.handlers=LoggingSynchronizationHandler

# [optional, default=[], availableOptions=[QueueSize,CollectionCount,DbHash]] 
# Collection equality verification performed periodically on each source and destination collection
perform.synchronization.detectors=QueueSize,CollectionCount,DbHash

# [optional, default=[], availableOptions=[DbAvailability,DestinationCollectionMissing,SourceCollectionAvailable]] 
# Validators used before starting migration
perform.synchronization.validators=DbAvailability,DestinationCollectionMissing,SourceCollectionAvailable

# [optional, default="/tmp/"] 
# Path to a directory where all mongo-migration-stream files will be saved
custom.rootPath=/tmp/mongomigrationstream/

# [optional, default=""] 
# Path to a directory where mongodump and mongorestore binaries are stored
custom.mongoToolsPath=/usr/local/bin/

# [optional, default="InMemoryCustomQueueFactory", availableOptions=[InMemoryCustomQueueFactory, BiqQueueFactory]] 
# What kind of queue should be used for storing synchronization events
custom.queue.factory=BiqQueueFactory

# [optional, default="primary", availableOptions=[primary, primaryPreferred, secondary, secondaryPreferred, nearest]] 
# Read preference used for mongodump
custom.dumpReadPreference=primary

# [optional, default=1000] 
# Maximum size of a batch which is send from mongo-migration-stream to the destination
custom.batchSize=1000

# [required]
# Ordered list of collection names from source database which should be migrated
collections.source=collection1,collection2

# [required]
# Ordered list of collection names where data from source collections should be migrated
collections.destination=collection1changedName,collection2changedName

# [required]
# URI of source database
source.db.uri=mongodb://mongo_rs36_1:36301,mongo_rs36_2:36302,mongo_rs36_3:36303/?replicaSet=replicaSet36

# [required]
# Name of source MongoDB database
source.db.name=test

# [optional, default=false]
# Should use authentication for connection with source database
source.db.authentication.enabled=true

# [(if source.db.authentication.enabled=true then required)]
# Username for source database
source.db.authentication.username=username

# [(if source.db.authentication.enabled=true then required)]
# Password for source database
source.db.authentication.password=password

# [(if source.db.authentication.enabled=true then required)]
# Source authentication database name
source.db.authentication.authDbName=admin

# [required]
# URI of destination database
destination.db.uri=mongodb://mongo_rs5_1:50301,mongo_rs5_2:50302,mongo_rs5_3:50303/?replicaSet=replicaSet5

# [required]
# Name of destination MongoDB database
destination.db.name=test

# [optional, default=false]
# Should use authentication for connection with destination database
destination.db.authentication.enabled=true

# [(if destination.db.authentication.enabled=true then required)]
# Username for destination database
destination.db.authentication.username=username

# [(if destination.db.authentication.enabled=true then required)]
# Password for destination database
destination.db.authentication.password=password

# [(if destination.db.authentication.enabled=true then required)]
# Destination authentication database name
destination.db.authentication.authDbName=admin
```
