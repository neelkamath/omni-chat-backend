package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Audio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object AudioMessages : Table() {
    override val tableName = "audio_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val audio: Column<ByteArray> = binary("audio", Audio.MAX_BYTES)

    /** @see Messages.createAudioMessage */
    fun create(messageId: Int, audio: Audio): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.audio] = audio.bytes
        }
    }

    fun read(messageId: Int): Audio =
        transaction { select(AudioMessages.messageId eq messageId).first()[audio].let(::Audio) }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
