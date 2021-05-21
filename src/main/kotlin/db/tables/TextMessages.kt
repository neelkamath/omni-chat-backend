package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object TextMessages : Table() {
    override val tableName = "text_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val text: Column<String> = varchar("text", MessageText.MAX_LENGTH)

    /** @see Messages.createTextMessage */
    fun create(messageId: Int, text: MessageText): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.text] = text.value
        }
    }

    fun read(messageId: Int): MessageText =
        transaction { select(TextMessages.messageId eq messageId).first()[text].let(::MessageText) }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
