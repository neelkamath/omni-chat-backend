package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Audio
import com.neelkamath.omniChat.db.PostgresEnum
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [Messages] */
object AudioMessages : Table() {
    override val tableName = "audio_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val audio: Column<ByteArray> = binary("audio", Audio.MAX_BYTES)
    private val type: Column<Audio.Type> = customEnumeration(
        name = "type",
        sql = "audio_type",
        fromDb = { Audio.Type.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("audio_type", it) }
    )

    /** @see [Messages.createAudioMessage] */
    fun create(id: Int, audio: Audio): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.audio] = audio.bytes
            it[this.type] = audio.type
        }
    }

    fun read(id: Int): Audio {
        val row = transaction {
            select { messageId eq id }.first()
        }
        return Audio(row[audio], row[type])
    }

    fun delete(idList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}
