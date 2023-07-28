package pl.allegro.tech.mongomigrationstream.logger

import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal object LoggerConfiguration {
    fun configureLogger(rootPath: String) {
        System.setProperty("logPath", Path.of(rootPath, "logs").absolutePathString())
    }
}
