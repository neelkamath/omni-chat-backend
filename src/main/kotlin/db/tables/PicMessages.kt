package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class CaptionedPic(val pic: Pic, val caption: MessageText?)

/** @see Messages */
object PicMessages : Table() {
    override val tableName = "pic_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val original: Column<ByteArray> = binary("original", Pic.ORIGINAL_MAX_BYTES)
    private val thumbnail: Column<ByteArray> = binary("thumbnail", Pic.THUMBNAIL_MAX_BYTES)
    private val caption: Column<String?> = varchar("caption", MessageText.MAX_LENGTH).nullable()

    /** @see Messages.createPicMessage */
    fun create(messageId: Int, message: CaptionedPic): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[original] = message.pic.original
            it[thumbnail] = message.pic.thumbnail
            it[caption] = message.caption?.value
        }
    }

    fun readCaption(messageId: Int): MessageText? =
        transaction { select(PicMessages.messageId eq messageId).first()[caption]?.let(::MessageText) }

    fun readCaptionedPic(messageId: Int): CaptionedPic =
        transaction { select(PicMessages.messageId eq messageId).first() }
            .let { CaptionedPic(Pic(it[original], it[thumbnail]), it[caption]?.let(::MessageText)) }

    fun readPic(messageId: Int, type: PicType): ByteArray {
        val pic = transaction { select(PicMessages.messageId eq messageId).first() }
        return when (type) {
            PicType.THUMBNAIL -> pic[thumbnail]
            PicType.ORIGINAL -> pic[original]
        }
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
