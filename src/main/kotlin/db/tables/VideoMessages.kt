package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.processing.Generated

/** An MP4 video. Throws an [IllegalArgumentException] if the [bytes] exceeds [Mp4.MAX_BYTES]. */
data class Mp4(
    /** At most [Mp4.MAX_BYTES]. */
    val bytes: ByteArray
) {
    init {
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("The video mustn't exceed $MAX_BYTES bytes.")
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mp4

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
object VideoMessages : Table() {
    override val tableName = "video_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val video: Column<ByteArray> = binary("audio", Mp4.MAX_BYTES)

    /** @see [Messages.createVideoMessage] */
    fun create(id: Int, video: Mp4): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.video] = video.bytes
        }
    }

    fun read(id: Int): Mp4 = transaction {
        select { messageId eq id }.first()[video].let(::Mp4)
    }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}