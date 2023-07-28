package pl.allegro.tech.mongomigrationstream.core.index

import pl.allegro.tech.mongomigrationstream.core.performer.IndexCloner

internal class NoOpIndexCloner : IndexCloner {
    override fun cloneIndexes() {}

    override fun stop() {
    }
}
