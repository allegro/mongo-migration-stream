package pl.allegro.tech.mongomigrationstream.core.transfer

import pl.allegro.tech.mongomigrationstream.core.performer.Transfer
import pl.allegro.tech.mongomigrationstream.core.performer.TransferResult
import pl.allegro.tech.mongomigrationstream.core.performer.TransferSuccess

internal class NoOpTransfer : Transfer {
    override fun performTransfer(): TransferResult = TransferSuccess
    override fun stop() {}
}
