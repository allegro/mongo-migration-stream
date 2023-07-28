package pl.allegro.tech.mongomigrationstream.core.detector

internal interface SynchronizationDetector {
    fun detect(): Set<DetectionResult>
}
