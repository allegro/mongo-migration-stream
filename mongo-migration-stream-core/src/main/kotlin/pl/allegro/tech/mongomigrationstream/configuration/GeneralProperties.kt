package pl.allegro.tech.mongomigrationstream.configuration

import pl.allegro.tech.mongomigrationstream.core.detector.handler.DetectionResultHandler

data class GeneralProperties(
    val shouldPerformTransfer: Boolean,
    val shouldPerformSynchronization: Boolean,
    val synchronizationHandlers: Set<DetectionResultHandler>,
    val synchronizationDetectors: Set<SynchronizationDetectorType>,
    val databaseValidators: Set<ValidatorType>
) {
    sealed class SynchronizationHandlerType
    object LoggingSynchronizationHandler : SynchronizationHandlerType()

    sealed class SynchronizationDetectorType
    object DbHashSynchronizationDetectorType : SynchronizationDetectorType()
    object QueueSizeSynchronizationDetectorType : SynchronizationDetectorType()
    object CollectionCountSynchronizationDetectorType : SynchronizationDetectorType()

    sealed class ValidatorType
    object DbAvailabilityValidatorType : ValidatorType()
    object DestinationMissingCollectionType : ValidatorType()
    object SourceCollectionAvailabilityType : ValidatorType()
    object MongoToolsValidatorType : ValidatorType()
}
