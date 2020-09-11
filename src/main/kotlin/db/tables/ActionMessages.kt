package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.graphql.routing.ActionMessageInput
import com.neelkamath.omniChat.graphql.routing.ActionableMessage
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.TriggeredAction
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [Messages] */
object ActionMessages : IntIdTable() {
    override val tableName = "action_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val text: Column<String> = varchar("text", MessageText.MAX_LENGTH)

    /** @see [Messages.createActionMessage] */
    fun create(messageId: Int, message: ActionMessageInput) {
        val actionMessageId = transaction {
            insertAndGetId {
                it[this.messageId] = messageId
                it[text] = message.text.value
            }.value
        }
        ActionMessageActions.create(actionMessageId, message.actions)
    }

    fun exists(messageId: Int): Boolean = transaction {
        select { ActionMessages.messageId eq messageId }.empty().not()
    }

    fun hasAction(messageId: Int, action: MessageText): Boolean {
        val id = transaction {
            select { ActionMessages.messageId eq messageId }.first()[ActionMessages.id].value
        }
        return action in ActionMessageActions.read(id)
    }

    /**
     * Has the [userId] trigger the [messageId]'s [action], and sends this [TriggeredAction] to the creator of the
     * [messageId] via [messagesNotifier].
     */
    fun trigger(userId: Int, messageId: Int, action: MessageText) {
        val creatorId = Messages.readTypedMessage(messageId).message.sender.id
        val account = Users.read(userId).toAccount()
        messagesNotifier.publish(MessagesAsset(creatorId) to TriggeredAction(messageId, action, account))
    }

    fun read(messageId: Int): ActionableMessage {
        val row = transaction {
            select { ActionMessages.messageId eq messageId }.first()
        }
        val actions = ActionMessageActions.read(row[id].value)
        return ActionableMessage(MessageText(row[text]), actions)
    }

    /** Returns the ID of every action message in the [messageIdList]. */
    private fun readIdList(messageIdList: List<Int>): List<Int> = transaction {
        select { messageId inList messageIdList }.map { it[ActionMessages.id].value }
    }

    fun delete(messageIdList: List<Int>) {
        ActionMessageActions.delete(readIdList(messageIdList))
        transaction {
            deleteWhere { messageId inList messageIdList }
        }
    }
}