package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.ReadPreference
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import pl.allegro.tech.mongomigrationstream.core.synchronization.ConstantValueBatchSizeProvider

internal class PerformerPropertiesTest : ShouldSpec({
    should("create PerformerProperties") {
        shouldNotThrowAny {
            PerformerProperties(
                rootPath = "/",
                mongoToolsPath = "/",
                queueFactory = PerformerProperties.InMemoryQueueFactoryType,
                dumpReadPreference = ReadPreference.primary(),
                batchSizeProvider = ConstantValueBatchSizeProvider(1000),
                isCompressionEnabled = false,
                insertionWorkersPerCollection = 1
            )
        }
    }

    should("throw IllegalArgumentException when creating PerformerProperties with invalid number of insertionWorkersPerCollection") {
        val invalidNumberOfInsertionWorkersPerCollection = -1
        shouldThrow<IllegalArgumentException> {
            PerformerProperties(
                rootPath = "/",
                mongoToolsPath = "/",
                queueFactory = PerformerProperties.InMemoryQueueFactoryType,
                dumpReadPreference = ReadPreference.primary(),
                batchSizeProvider = ConstantValueBatchSizeProvider(1000),
                isCompressionEnabled = false,
                insertionWorkersPerCollection = invalidNumberOfInsertionWorkersPerCollection
            )
        }
    }
})
