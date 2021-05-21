package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** @see ActionMessages */
object ActionMessageActions : IntIdTable() {
    override val tableName = "action_message_actions"
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)
    private val action: Column<String> = varchar("action", MessageText.MAX_LENGTH)

    /** The [actions] can be [read] in the order they were created in. */
    fun create(messageId: Int, actions: LinkedHashSet<MessageText>): Unit = transaction {
        batchInsert(actions) {
            this[ActionMessageActions.messageId] = messageId
            this[action] = it.value
        }
    }

    /** Returns the [messageId]'s actions with the same order they were created with. */
    fun read(messageId: Int): LinkedHashSet<MessageText> = transaction {
        select(ActionMessageActions.messageId eq messageId)
            .orderBy(ActionMessageActions.id)
            .map { MessageText(it[action]) }
            .toLinkedHashSet()
    }

    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
