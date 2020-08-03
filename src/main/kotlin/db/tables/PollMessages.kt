package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.messagesBroker
import com.neelkamath.omniChat.graphql.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/** @see [Messages] */
object PollMessages : IntIdTable() {
    override val tableName = "poll_messages"
    private val messageId: Column<Int> = integer("message_id").uniqueIndex().references(Messages.id)
    private val title: Column<String> = varchar("title", MessageText.MAX_LENGTH)

    /** @see [Messages.create] */
    fun create(messageId: Int, poll: PollInput) {
        val pollId = transaction {
            insertAndGetId {
                it[this.messageId] = messageId
                it[title] = poll.title.value
            }.value
        }
        PollOptions.create(pollId, poll.options)
    }

    fun read(messageId: Int): Poll {
        val row = transaction {
            select { PollMessages.messageId eq messageId }.first()
        }
        val options = PollOptions.read(row[id].value)
        return Poll(MessageText(row[title]), options)
    }

    /**
     * Sets the [userId]'s vote on the [messageId]'s [option]. If [vote], a vote will be added if the [userId] hasn't
     * already added one. Otherwise, the [userId]'s vote will be removed if it exists.
     */
    fun setVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean) {
        val optionId = PollOptions.readId(readId(messageId), option)
        if (vote) PollVotes.create(userId, optionId) else PollVotes.deleteVote(userId, optionId)
        messagesBroker.notify({ UpdatedMessage.build(it.userId, messageId) as MessagesSubscription }) {
            isUserInChat(it.userId, Messages.readChatFromMessage(messageId))
        }
    }

    /** Whether the [messageId] has the [option]. */
    fun hasOption(messageId: Int, option: MessageText): Boolean {
        val pollId = transaction {
            select { PollMessages.messageId eq messageId }.first()[PollMessages.id].value
        }
        return PollOptions.read(pollId).any { it.option == option }
    }

    /** Returns the poll ID of the [messageId]. */
    private fun readId(messageId: Int): Int = transaction {
        select { PollMessages.messageId eq messageId }.first()[PollMessages.id].value
    }

    /** Returns the ID of every poll message in the [messageIdList]. */
    private fun readIdList(messageIdList: List<Int>): List<Int> = transaction {
        select { messageId inList messageIdList }.map { it[PollMessages.id].value }
    }

    fun delete(messageIdList: List<Int>) {
        PollOptions.delete(readIdList(messageIdList))
        transaction {
            deleteWhere { messageId inList messageIdList }
        }
    }
}