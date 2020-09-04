package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.MessageText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [ActionMessages] */
object ActionMessageActions : Table() {
    override val tableName = "action_message_actions"
    private val actionMessageId: Column<Int> = integer("action_message_id").references(ActionMessages.id)
    private val action: Column<String> = varchar("action", MessageText.MAX_LENGTH)

    fun create(actionMessageId: Int, actions: List<MessageText>): Unit = transaction {
        batchInsert(actions) {
            this[ActionMessageActions.actionMessageId] = actionMessageId
            this[action] = it.value
        }
    }

    fun read(actionMessageId: Int): List<MessageText> = transaction {
        select { ActionMessageActions.actionMessageId eq actionMessageId }.map { MessageText(it[action]) }
    }

    fun delete(actionMessageIdList: List<Int>): Unit = transaction {
        deleteWhere { actionMessageId inList actionMessageIdList }
    }
}