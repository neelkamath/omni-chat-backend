package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.DocFile
import com.neelkamath.omniChatBackend.db.Filename
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object DocMessages : Table() {
    override val tableName = "doc_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val filename: Column<Filename> = varchar("filename", 255)
    private val bytes: Column<ByteArray> = binary("bytes", DocFile.MAX_BYTES)

    /** @see Messages.createDocMessage */
    fun create(messageId: Int, file: DocFile): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.filename] = file.filename
            it[this.bytes] = file.bytes
        }
    }

    fun read(messageId: Int): DocFile {
        val row = transaction { select(DocMessages.messageId eq messageId).first() }
        return DocFile(row[filename], row[bytes])
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
