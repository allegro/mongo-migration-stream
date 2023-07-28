package pl.allegro.tech.mongomigrationstream.infrastructure.queue

import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties.BiqQueueFactoryType
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties.InMemoryQueueFactoryType
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueueAbstractFactory
import pl.allegro.tech.mongomigrationstream.core.synchronization.ChangeEvent

internal object QueueFactory {
    fun createQueues(properties: ApplicationProperties): Map<SourceToDestination, EventQueue<ChangeEvent>> {
        val queueFactory: EventQueueAbstractFactory<ChangeEvent> = when (properties.performerProperties.queueFactory) {
            InMemoryQueueFactoryType -> InMemoryEventQueueFactory()
            BiqQueueFactoryType -> BigQueueEventQueueFactory(properties.performerProperties.rootPath)
        }
        return properties.sourceToDestinationMapping.associateWith { queueFactory.produce(it.source.toString()) }
    }
}
