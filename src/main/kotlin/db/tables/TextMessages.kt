package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.TextMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** IDs refer to [Messages.id]s. */
object TextMessages : Table() {
    private val id: Column<Int> = integer("id").uniqueIndex().references(Messages.id)
    private val text: Column<String> = varchar("text", TextMessage.MAX_LENGTH)

    fun create(id: Int, text: TextMessage): Unit = transaction {
        insert {
            it[this.id] = id
            it[this.text] = text.value
        }
    }

    fun read(id: Int): TextMessage = transaction {
        select { TextMessages.id eq id }.first()[text].let(::TextMessage)
    }

    fun delete(idList: List<Int>): Unit = transaction {
        deleteWhere { TextMessages.id inList idList }
    }
}