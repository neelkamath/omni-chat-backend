package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.chatMessagesNotifier
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** @see Messages */
object GroupChatInviteMessages : Table() {
    override val tableName = "group_chat_invite_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)

    /** @see Messages.createGroupChatInviteMessage */
    fun create(messageId: Int, groupChatId: Int): Unit = transaction {
        insert {
            it[this.messageId] = messageId
            it[this.groupChatId] = groupChatId
        }
    }

    /** Returns the ID of the invited chat. */
    fun read(messageId: Int): Int =
        transaction { select(GroupChatInviteMessages.messageId eq messageId).first()[groupChatId] }

    /** @see deleteChat */
    fun deleteMessages(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /**
     * Deletes every invitation for the [chatId].
     *
     * Subscribers will be notified of the [DeletedMessage]s via [messagesNotifier]. If a message is from a public group
     * chat, then subscribers will be notified via [chatMessagesNotifier] as well.
     *
     * @see deleteMessages
     */
    fun deleteChat(chatId: Int) {
        val messageIdList = transaction {
            select(groupChatId eq chatId).map { it[messageId] }
        }
        Messages.delete(messageIdList)
    }
}
