package pl.allegro.tech.mongomigrationstream.metrics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.lang.Nullable
import io.micrometer.jmx.JmxConfig
import io.micrometer.jmx.JmxMeterRegistry
import java.time.Duration

private const val METRIC_REPORT_DURATION = 10L

internal object JmxConfiguration {

    val jmxMeterRegistry = JmxMeterRegistry(
        object : JmxConfig {
            override fun step(): Duration = Duration.ofSeconds(METRIC_REPORT_DURATION)

            @Nullable
            override fun get(k: String): String? = null
        },
        Clock.SYSTEM
    )
}
