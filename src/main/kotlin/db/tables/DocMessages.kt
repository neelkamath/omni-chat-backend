package com.neelkamath.omniChat.db.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.processing.Generated

/** An [IllegalArgumentException] will be thrown if the [bytes] exceeds [Doc.MAX_BYTES]. */
data class Doc(val bytes: ByteArray) {
    init {
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("The doc cannot exceed $MAX_BYTES bytes.")
    }

    companion object {
        /** Docs cannot exceed 5 MiB. */
        const val MAX_BYTES = 5 * 1024 * 1024
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

/** @see [Messages] */
object DocMessages : Table() {
    override val tableName = "doc_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val doc: Column<ByteArray> = binary("doc", Doc.MAX_BYTES)

    /** @see [Messages.createDocMessage] */
    fun create(id: Int, doc: Doc): Unit = transaction {
        insert {
            it[this.messageId] = id
            it[this.doc] = doc.bytes
        }
    }

    fun read(id: Int): Doc = transaction {
        select { messageId eq id }.first()[doc].let(::Doc)
    }

    fun delete(idList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList idList }
    }
}
