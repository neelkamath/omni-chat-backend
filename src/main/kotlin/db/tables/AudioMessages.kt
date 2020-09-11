package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.processing.Generated

/** An MP3 audio. Throws an [IllegalArgumentException] if the [bytes] exceeds [Mp3.MAX_BYTES]. */
data class Mp3(
    /** At most [Mp3.MAX_BYTES]. */
    val bytes: ByteArray
) {
    init {
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("The audio mustn't exceed $MAX_BYTES bytes.")
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mp3

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    companion object {
        const val MAX_BYTES = 25 * 1024 * 1024
    }
}

/** @see [Messages] */
object AudioMessages : Table() {
    override val tableName = "audio_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val audio: Column<ByteArray> = binary("audio", Mp3.MAX_BYTES)

    /** @see [Messages.createAudioMessage] */
    fun create(id: Int, audio: Mp3): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.audio] = audio.bytes
        }
    }

    fun read(id: Int): Mp3 = transaction {
        select { messageId eq id }.first()[audio].let(::Mp3)
    }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}