package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TriggeredAction
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @see Messages
 * @see ActionMessageActions
 */
object ActionMessages : Table() {
    override val tableName = "action_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val text: Column<String> = varchar("text", MessageText.MAX_LENGTH)

    /** @see Messages.createActionMessage */
    fun create(messageId: Int, message: ActionMessageInput) {
        transaction {
            insert {
                it[this.messageId] = messageId
                it[text] = message.text.value
            }
        }
        ActionMessageActions.create(messageId, message.actions.toLinkedHashSet())
    }

    fun isExisting(messageId: Int): Boolean =
        transaction { select(ActionMessages.messageId eq messageId).empty().not() }

    fun hasAction(messageId: Int, action: MessageText): Boolean = action in ActionMessageActions.read(messageId)

    /**
     * Returns `false` in the following cases:
     * - The [messageId] isn't [Messages.isVisible].
     * - The [userId] isn't in the chat the [messageId] is from.
     * - The [action] doesn't exist on the [messageId].
     */
    fun isValidTrigger(userId: Int, messageId: Int, action: MessageText): Boolean {
        if (!Messages.isVisible(userId, messageId))
            return false
        val chatId = Messages.readChatId(messageId)
        if (GroupChats.isExistingPublicChat(chatId) && chatId !in GroupChatUsers.readChatIdList(userId))
            return false
        if (!hasAction(messageId, action))
            return false
        return true
    }

    /**
     * Has the [userId] trigger the [messageId]'s [action], and sends this [TriggeredAction] to the creator of the
     * [messageId] via [messagesNotifier].
     *
     * An [IllegalArgumentException] gets thrown if not [isValidTrigger].
     */
    fun trigger(userId: Int, messageId: Int, action: MessageText) {
        if (!isValidTrigger(userId, messageId, action))
            throw IllegalArgumentException(
                "The user (ID: $userId) cannot trigger the action ($action) on the message (ID: $messageId).",
            )
        val senderId = Messages.readSenderId(messageId)
        messagesNotifier.publish(TriggeredAction(messageId, action, userId), UserId(senderId))
    }

    fun readText(messageId: Int): MessageText =
        transaction { select(ActionMessages.messageId eq messageId).first()[text].let(::MessageText) }

    fun delete(messageIdList: Collection<Int>) {
        ActionMessageActions.delete(messageIdList)
        transaction {
            deleteWhere { messageId inList messageIdList }
        }
    }
}
