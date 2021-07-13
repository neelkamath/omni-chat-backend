package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.AudioFile
import com.neelkamath.omniChatBackend.db.Filename
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object AudioMessages : Table() {
    override val tableName = "audio_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val filename: Column<Filename> = varchar("filename", 255)
    private val bytes: Column<ByteArray> = binary("bytes", AudioFile.MAX_BYTES)

    /** @see Messages.createAudioMessage */
    fun create(messageId: Int, file: AudioFile): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.filename] = file.filename
            it[this.bytes] = file.bytes
        }
    }

    fun read(messageId: Int): AudioFile {
        val row = transaction { select(AudioMessages.messageId eq messageId).first() }
        return AudioFile(row[filename], row[bytes])
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
