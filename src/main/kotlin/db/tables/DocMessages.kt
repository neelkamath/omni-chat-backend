package com.neelkamath.omniChatBackend.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.processing.Generated

/** An [IllegalArgumentException] will be thrown if the [bytes] exceeds [Doc.MAX_BYTES]. */
data class Doc(val bytes: ByteArray) {
    init {
        require(bytes.size <= MAX_BYTES) { "The doc cannot exceed $MAX_BYTES bytes." }
    }

    companion object {
        /** Docs cannot exceed 3 MiB. */
        const val MAX_BYTES = 3 * 1_024 * 1_024
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Doc

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

/** @see Messages */
object DocMessages : Table() {
    override val tableName = "doc_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val doc: Column<ByteArray> = binary("doc", Doc.MAX_BYTES)

    /** @see Messages.createDocMessage */
    fun create(messageId: Int, doc: Doc): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.doc] = doc.bytes
        }
    }

    fun read(messageId: Int): Doc = transaction { select(DocMessages.messageId eq messageId).first()[doc].let(::Doc) }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
