package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.PostgresEnum
import com.neelkamath.omniChat.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class CaptionedPic(val pic: Pic, val caption: MessageText?)

/** @see [Messages] */
object PicMessages : Table() {
    override val tableName = "pic_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val pic: Column<ByteArray> = binary("pic", Pic.MAX_BYTES)
    private val type: Column<Pic.Type> = customEnumeration(
            name = "type",
            sql = "pic_type",
            fromDb = { Pic.Type.valueOf((it as String).toUpperCase()) },
            toDb = { PostgresEnum("pic_type", it) }
    )
    private val caption: Column<String?> = varchar("caption", MessageText.MAX_LENGTH).nullable()

    /** @see [Messages.createPicMessage] */
    fun create(id: Int, message: CaptionedPic): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[pic] = message.pic.bytes
            it[type] = message.pic.type
            it[caption] = message.caption?.value
        }
    }

    fun read(id: Int): CaptionedPic = transaction {
        select { messageId eq id }.first()
    }.let { CaptionedPic(Pic(it[pic], it[type]), it[caption]?.let(::MessageText)) }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}
