package pl.allegro.tech.mongomigrationstream.core.synchronization

import java.time.Duration

const val TIMEOUT = 10L
const val MIN_DELAY = 1L
const val MAX_DELAY = 256L
const val RETRIES = -1

internal object SynchronizationParameters {
    const val INFINITE_RETRIES = RETRIES
    val BULK_WRITE_TIMEOUT: Duration = Duration.ofSeconds(TIMEOUT)
    val BULK_WRITE_BACKOFF_MIN_DELAY: Duration = Duration.ofMinutes(MIN_DELAY)
    val BULK_WRITE_BACKOFF_MAX_DELAY: Duration = Duration.ofMinutes(MAX_DELAY)
}
