package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Filename
import com.neelkamath.omniChatBackend.db.VideoFile
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object VideoMessages : Table() {
    override val tableName = "video_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val filename: Column<Filename> = varchar("filename", 255)
    private val bytes: Column<ByteArray> = binary("bytes", VideoFile.MAX_BYTES)

    /** @see Messages.createVideoMessage */
    fun create(messageId: Int, file: VideoFile): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.filename] = file.filename
            it[this.bytes] = file.bytes
        }
    }

    fun read(messageId: Int): VideoFile {
        val row = transaction { select(VideoMessages.messageId eq messageId).first() }
        return VideoFile(row[filename], row[bytes])
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
