package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.TextMessage
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.PostgresEnum
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class PicMessage(val pic: Pic, val caption: TextMessage?)

/** IDs refer to [Messages.id]s. */
object PicMessages : Table() {
    private val id: Column<Int> = integer("id").uniqueIndex().references(Messages.id)
    private val pic: Column<ByteArray> = binary("pic", Pic.MAX_BYTES)
    private val type: Column<Pic.Type> = customEnumeration(
        name = "type",
        sql = "pic_type",
        fromDb = { Pic.Type.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("pic_type", it) }
    )
    private val caption: Column<String?> = varchar("caption", TextMessage.MAX_LENGTH).nullable()

    fun create(id: Int, message: PicMessage): Unit = transaction {
        insert {
            it[this.id] = id
            it[pic] = message.pic.bytes
            it[type] = message.pic.type
            it[caption] = message.caption?.value
        }
    }

    fun read(id: Int): PicMessage = transaction {
        select { PicMessages.id eq id }.first()
    }.let { PicMessage(Pic(it[pic], it[type]), it[caption]?.let(::TextMessage)) }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { PicMessages.id inList idList }
    }
}