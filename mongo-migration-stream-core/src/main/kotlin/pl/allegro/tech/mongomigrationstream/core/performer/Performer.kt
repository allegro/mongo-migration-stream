package pl.allegro.tech.mongomigrationstream.core.performer

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.StopEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo

internal class Performer(
    private val sourceToLocalSynchronizer: Synchronizer,
    private val transfer: Transfer,
    private val localToDestinationSynchronizer: ResumableSynchronizer,
    private val indexCloner: IndexCloner,
    private val sourceToDestination: SourceToDestination,
    private val stateInfo: StateInfo
) {
    fun perform(): PerformerResult {
        val sourceToLocalResult = sourceToLocalSynchronizer.startSynchronization()
        val transferResult = transfer.performTransfer()
        indexCloner.cloneIndexes()
        val localToDestinationResult = localToDestinationSynchronizer.startSynchronization()
        return PerformerResult(sourceToLocalResult, transferResult, localToDestinationResult)
    }

    fun pause() {
        localToDestinationSynchronizer.pause()
    }

    fun resume() {
        localToDestinationSynchronizer.resume()
    }

    fun stop() {
        stateInfo.notifyStateChange(StopEvent(sourceToDestination))
        transfer.stop()
        indexCloner.stop()
        sourceToLocalSynchronizer.stop()
        localToDestinationSynchronizer.stop()
    }
}

internal interface Transfer {
    fun performTransfer(): TransferResult
    fun stop()
}

internal interface Synchronizer {
    fun startSynchronization(): SynchronizationResult
    fun stop()
}

internal interface ResumableSynchronizer : Synchronizer {
    fun pause()
    fun resume()
}

internal interface IndexCloner {
    fun cloneIndexes()
    fun stop()
}

internal sealed class TransferResult
internal object TransferSuccess : TransferResult()
internal data class TransferFailure(
    val cause: Throwable? = null
) : TransferResult()

internal sealed class SynchronizationResult
internal object SynchronizationSuccess : SynchronizationResult()
internal data class SynchronizationFailure(
    val cause: Throwable
) : SynchronizationResult()

internal data class PerformerResult(
    val sourceToLocalResult: SynchronizationResult,
    val transferResult: TransferResult,
    val synchronizationResult: SynchronizationResult
) {
    fun isSuccessful(): Boolean =
        sourceToLocalResult is SynchronizationSuccess && transferResult is TransferSuccess && synchronizationResult is SynchronizationSuccess
}
