package pl.allegro.tech.mongomigrationstream.infrastructure.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.detector.DetectionResult
import pl.allegro.tech.mongomigrationstream.core.detector.handler.DetectionResultHandler

private val logger = KotlinLogging.logger { }

object LoggingDetectionResultHandler : DetectionResultHandler {
    override fun handle(result: DetectionResult) {
        logger.info { result.toString() }
    }
}
