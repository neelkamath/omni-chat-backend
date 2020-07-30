package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.Generated

/** An MP3 audio. Throws an [IllegalArgumentException] if the [bytes] exceeds [AudioMessages.MAX_AUDIO_BYTES]. */
data class Mp3(
    /** At most [AudioMessages.MAX_AUDIO_BYTES]. */
    val bytes: ByteArray
) {
    init {
        if (bytes.size > AudioMessages.MAX_AUDIO_BYTES)
            throw IllegalArgumentException("The audio mustn't exceed ${AudioMessages.MAX_AUDIO_BYTES} bytes.")
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
}

/** IDs refer to [Messages.id]s. */
object AudioMessages : Table() {
    /** An audio message cannot exceed 25 MiB. */
    const val MAX_AUDIO_BYTES = 25 * 1024 * 1024

    private val id: Column<Int> = integer("id").uniqueIndex().references(Messages.id)
    private val audio: Column<ByteArray> = binary("audio", MAX_AUDIO_BYTES)

    fun create(id: Int, audio: Mp3): Unit = transaction {
        insert {
            it[this.id] = id
            it[this.audio] = audio.bytes
        }
    }

    fun read(id: Int): Mp3 = transaction {
        select { AudioMessages.id eq id }.first()[audio].let(::Mp3)
    }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { AudioMessages.id inList idList }
    }
}