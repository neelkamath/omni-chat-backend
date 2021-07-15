package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Filename
import com.neelkamath.omniChatBackend.db.ImageFile
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class CaptionedImage(val image: ProcessedImage, val caption: MessageText?)

/** @see Messages */
object ImageMessages : Table() {
    override val tableName = "image_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val filename: Column<Filename> = varchar("filename", 255)
    private val original: Column<ByteArray> = binary("original", ProcessedImage.ORIGINAL_MAX_BYTES)
    private val thumbnail: Column<ByteArray> = binary("thumbnail", ProcessedImage.THUMBNAIL_MAX_BYTES)
    private val caption: Column<String?> = varchar("caption", MessageText.MAX_LENGTH).nullable()

    /** @see Messages.createImageMessage */
    fun create(messageId: Int, message: CaptionedImage): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.filename] = message.image.filename
            it[original] = message.image.original
            it[thumbnail] = message.image.thumbnail
            it[caption] = message.caption?.value
        }
    }

    fun readCaption(messageId: Int): MessageText? =
        transaction { select(ImageMessages.messageId eq messageId).first()[caption]?.let(::MessageText) }

    fun readCaptionedImage(messageId: Int): CaptionedImage {
        val row = transaction { select(ImageMessages.messageId eq messageId).first() }
        val image = ProcessedImage(row[filename], row[original], row[thumbnail])
        return CaptionedImage(image, row[caption]?.let(::MessageText))
    }

    fun readImage(messageId: Int, type: ImageType): ImageFile {
        val row = transaction { select(ImageMessages.messageId eq messageId).first() }
        val bytes = when (type) {
            ImageType.THUMBNAIL -> row[thumbnail]
            ImageType.ORIGINAL -> row[original]
        }
        return ImageFile(row[filename], bytes)
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
