package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedPollMessage
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.PollInput
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @see Messages
 * @see PollMessageOptions
 */
object PollMessages : Table() {
    override val tableName = "poll_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val question: Column<String> = varchar("question", MessageText.MAX_LENGTH)

    /** @see Messages.createPollMessage */
    fun create(messageId: Int, poll: PollInput) {
        transaction {
            insert {
                it[this.messageId] = messageId
                it[question] = poll.question.value
            }
        }
        PollMessageOptions.create(messageId, poll.options.toLinkedHashSet())
    }

    fun isExisting(messageId: Int): Boolean = transaction { select(PollMessages.messageId eq messageId).empty().not() }

    fun readQuestion(messageId: Int): MessageText =
        transaction { select(PollMessages.messageId eq messageId).first()[question].let(::MessageText) }

    /**
     * Sets the [userId]'s vote on the [messageId]'s [option]. If [vote], a vote will be added if the [userId] hasn't
     * already added one. Otherwise, the [userId]'s vote will be removed if it exists.
     *
     * Subscribers will be notified via [messagesNotifier] of the [UpdatedPollMessage]. If the [messageId] belongs to a
     * public group chat, then subscribers will also be notified via [chatMessagesNotifier].
     */
    fun setVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean) {
        val optionId = PollMessageOptions.readOptionId(messageId, option)
        if (vote) PollMessageVotes.create(userId, optionId) else PollMessageVotes.deleteVote(userId, optionId)
        val chatId = Messages.readChatId(messageId)
        val update = UpdatedPollMessage(messageId)
        messagesNotifier.publish(update, readUserIdList(chatId).map(::UserId))
        if (GroupChats.isExistingPublicChat(chatId)) chatMessagesNotifier.publish(update, ChatId(chatId))
    }

    fun delete(messageIdList: Collection<Int>) {
        PollMessageOptions.delete(messageIdList)
        transaction {
            deleteWhere { messageId inList messageIdList }
        }
    }
}
