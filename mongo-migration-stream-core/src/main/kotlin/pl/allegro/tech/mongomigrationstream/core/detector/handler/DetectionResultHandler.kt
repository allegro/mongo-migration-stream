package pl.allegro.tech.mongomigrationstream.core.detector.handler

import pl.allegro.tech.mongomigrationstream.core.detector.DetectionResult

interface DetectionResultHandler {
    fun handle(result: DetectionResult)
}
