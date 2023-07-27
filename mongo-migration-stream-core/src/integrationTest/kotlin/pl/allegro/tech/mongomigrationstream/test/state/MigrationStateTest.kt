package pl.allegro.tech.mongomigrationstream.test.state

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import pl.allegro.tech.mongomigrationstream.MongoMigrationStream
import pl.allegro.tech.mongomigrationstream.core.state.State
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.DUMP
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.FINISHED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.INDEX_REBUILD
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.LOCAL_TO_DESTINATION
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.NEW
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.PAUSED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.RESTORE
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.RESUMED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.SOURCE_TO_LOCAL
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.destinationDb
import pl.allegro.tech.mongomigrationstream.utils.sameCollectionMongoMigrationStream
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import pl.allegro.tech.mongomigrationstream.utils.testDocument
import java.util.concurrent.TimeUnit.SECONDS

internal class MigrationStateTest : ShouldSpec({
    should("aggregate state when running migration") {
        // given: empty collection in source Mongo
        val collectionName = "newStateTestCollection"
        sourceDb.createCollection(collectionName)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)

        mms.start()
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }

        // then: should change state to New
        migrationStepsTypes(mms).first().shouldBe(NEW)

        // when: events are migrated
        sourceCollection(collectionName).insertOne(testDocument)
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }

        // then: should aggregate complete state
        with(migrationStepsTypes(mms)) {
            get(1).shouldBe(SOURCE_TO_LOCAL)
            get(2).shouldBe(DUMP)
            get(3).shouldBe(RESTORE)
            get(4).shouldBe(INDEX_REBUILD)
            get(5).shouldBe(LOCAL_TO_DESTINATION)
        }

        // when: migration is paused
        mms.pause()

        // then: state should be paused
        migrationStepsTypes(mms).last().shouldBe(PAUSED)

        // when: migration is resumed
        mms.resume()

        // then: state should be resumed
        migrationStepsTypes(mms).last().shouldBe(RESUMED)

        // when: migration is stopped
        mms.stop()

        // then: state should be finished
        migrationStepsTypes(mms).last().shouldBe(FINISHED)
    }
})

private fun migrationStepsTypes(mms: MongoMigrationStream): List<State.StepType> {
    return mms.stateInfo.getMigrationState().collectionStates.first().steps.map { it.type }
}
