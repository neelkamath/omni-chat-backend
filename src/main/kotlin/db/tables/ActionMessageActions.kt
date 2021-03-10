package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.MessageText
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [ActionMessages] */
object ActionMessageActions : IntIdTable() {
    override val tableName = "action_message_actions"
    private val actionMessageId: Column<Int> = integer("action_message_id").references(ActionMessages.id)
    private val action: Column<String> = varchar("action", MessageText.MAX_LENGTH)

    fun create(actionMessageId: Int, actions: LinkedHashSet<MessageText>): Unit = transaction {
        batchInsert(actions) {
            this[ActionMessageActions.actionMessageId] = actionMessageId
            this[action] = it.value
        }
    }

    fun read(actionMessageId: Int): LinkedHashSet<MessageText> = transaction {
        select { ActionMessageActions.actionMessageId eq actionMessageId }
            .orderBy(ActionMessageActions.id)
            .map { MessageText(it[action]) }
            .toSet() as LinkedHashSet
    }

    fun delete(actionMessageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { actionMessageId inList actionMessageIdList }
    }
}
