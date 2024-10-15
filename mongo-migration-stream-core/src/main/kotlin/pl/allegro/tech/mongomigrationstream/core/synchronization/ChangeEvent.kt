package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.DBRefCodecProvider
import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import com.mongodb.client.model.WriteModel
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.OperationType
import com.mongodb.client.model.changestream.OperationType.DELETE
import com.mongodb.client.model.changestream.OperationType.INSERT
import com.mongodb.client.model.changestream.OperationType.REPLACE
import com.mongodb.client.model.changestream.OperationType.UPDATE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.BsonDocument
import org.bson.codecs.BsonCodecProvider
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.EnumCodecProvider
import org.bson.codecs.IterableCodecProvider
import org.bson.codecs.JsonObjectCodecProvider
import org.bson.codecs.MapCodecProvider
import org.bson.codecs.ValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.Jsr310CodecProvider
import java.io.Serializable

private val logger = KotlinLogging.logger { }

internal sealed class ChangeEvent(
    open val operationType: OperationType,
    open val documentKey: BsonDocument
) : Serializable {
    fun toWriteModel(): WriteModel<BsonDocument>? = try {
        toWriteModelImpl()
    } catch (exception: NullPointerException) {
        logger.error(exception) { "Error when trying to convert ChangeEvent to WriteModel. ChangeEvent: [$this]" }
        null
    }

    protected abstract fun toWriteModelImpl(): WriteModel<BsonDocument>

    companion object {
        fun fromMongoChangeStreamDocument(changeStreamDocument: ChangeStreamDocument<BsonDocument>): ChangeEvent =
            when (changeStreamDocument.operationType) {
                INSERT -> InsertChangeEvent.fromMongoChangeStreamDocument(changeStreamDocument)
                REPLACE -> ReplaceChangeEvent.fromMongoChangeStreamDocument(changeStreamDocument)
                UPDATE -> UpdateChangeEvent.fromMongoChangeStreamDocument(changeStreamDocument)
                DELETE -> DeleteChangeEvent.fromMongoChangeStreamDocument(changeStreamDocument)
                else -> throw IllegalArgumentException("Not supported operation type: [${changeStreamDocument.operationType}]")
            }
    }
}

private val writeCodecProviders = listOf(
    ValueCodecProvider(),
    BsonValueCodecProvider(),
    DocumentCodecProvider(),
    IterableCodecProvider(),
    MapCodecProvider(),
    Jsr310CodecProvider(),
    JsonObjectCodecProvider(),
    BsonCodecProvider(),
    EnumCodecProvider(),
    DBRefCodecProvider() // DBRefCodecProvider is missing in DEFAULT_CODEC_REGISTRY
)

internal data class InsertChangeEvent(
    override val operationType: OperationType,
    override val documentKey: BsonDocument,
    val document: BsonDocument?,
) : ChangeEvent(operationType, documentKey) {
    companion object {
        private val codecRegistry: CodecRegistry = fromProviders(writeCodecProviders)

        fun fromMongoChangeStreamDocument(changeStreamDocument: ChangeStreamDocument<BsonDocument>): InsertChangeEvent =
            InsertChangeEvent(
                changeStreamDocument.operationType,
                changeStreamDocument.documentKey!!,
                changeStreamDocument.fullDocument?.toBsonDocument(
                    BsonDocument::class.java,
                    codecRegistry
                )
            )
    }

    override fun toWriteModelImpl(): WriteModel<BsonDocument> = InsertOneModel(document!!)
}

internal data class ReplaceChangeEvent(
    override val operationType: OperationType,
    override val documentKey: BsonDocument,
    val document: BsonDocument?,
) : ChangeEvent(operationType, documentKey) {
    companion object {
        private val codecRegistry: CodecRegistry = fromProviders(writeCodecProviders)

        fun fromMongoChangeStreamDocument(changeStreamDocument: ChangeStreamDocument<BsonDocument>): ReplaceChangeEvent =
            ReplaceChangeEvent(
                changeStreamDocument.operationType,
                changeStreamDocument.documentKey!!,
                changeStreamDocument.fullDocument?.toBsonDocument(
                    BsonDocument::class.java,
                    codecRegistry
                )
            )
    }

    override fun toWriteModelImpl(): WriteModel<BsonDocument> = ReplaceOneModel(
        documentKey,
        document!!,
        ReplaceOptions().upsert(true)
    )
}

internal data class DeleteChangeEvent(
    override val operationType: OperationType,
    override val documentKey: BsonDocument
) : ChangeEvent(operationType, documentKey) {
    companion object {
        fun fromMongoChangeStreamDocument(changeStreamDocument: ChangeStreamDocument<BsonDocument>): DeleteChangeEvent =
            DeleteChangeEvent(
                changeStreamDocument.operationType,
                changeStreamDocument.documentKey!!
            )
    }

    override fun toWriteModelImpl(): WriteModel<BsonDocument> = DeleteOneModel(documentKey)
}

internal data class UpdateChangeEvent(
    override val operationType: OperationType,
    override val documentKey: BsonDocument,
    val removedFields: List<String>,
    val updatedFields: BsonDocument,
) : ChangeEvent(operationType, documentKey) {

    companion object {
        fun fromMongoChangeStreamDocument(changeStreamDocument: ChangeStreamDocument<BsonDocument>): UpdateChangeEvent {
            if (changeStreamDocument.updateDescription?.truncatedArrays?.isNotEmpty() == true) {
                logger.error { "Cannot handle truncatedArrays from Mongo Change Event. Event itself: [$changeStreamDocument]" }
            }
            return UpdateChangeEvent(
                changeStreamDocument.operationType,
                changeStreamDocument.documentKey!!,
                changeStreamDocument.updateDescription?.removedFields ?: emptyList(),
                changeStreamDocument.updateDescription?.updatedFields!!
            )
        }
    }

    override fun toWriteModelImpl(): WriteModel<BsonDocument> = UpdateOneModel(
        documentKey,
        Updates.combine(
            *updatedFields.entries.map { Updates.set(it.key, it.value) }.toTypedArray(),
            *removedFields.map { Updates.unset(it) }.toTypedArray()
        )
    )
}
