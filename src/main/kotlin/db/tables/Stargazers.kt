package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.messagesBroker
import com.neelkamath.omniChat.graphql.routing.MessagesSubscription
import com.neelkamath.omniChat.graphql.routing.UpdatedMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** Users who have starred [Messages]. */
object Stargazers : Table() {
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)

    /** If [hasStar], nothing happens. [Broker.notify]s the [userId] of the starred [messageId] via [messagesBroker]. */
    fun create(userId: Int, messageId: Int) {
        if (hasStar(userId, messageId)) return
        transaction {
            insert {
                it[this.userId] = userId
                it[this.messageId] = messageId
            }
        }
        val message = UpdatedMessage.build(userId, messageId) as MessagesSubscription
        messagesBroker.notify(message) { it.userId == userId }
    }

    /** Returns the ID of every message the [userId] has starred. */
    fun read(userId: Int): List<Int> = transaction {
        select { Stargazers.userId eq userId }.map { it[messageId] }
    }

    /** Returns the ID of every user who has starred the [messageId]. */
    private fun readStargazers(messageId: Int): List<Int> = transaction {
        select { Stargazers.messageId eq messageId }.map { it[userId] }
    }

    fun hasStar(userId: Int, messageId: Int): Boolean = transaction {
        select { (Stargazers.userId eq userId) and (Stargazers.messageId eq messageId) }.empty().not()
    }

    /**
     * Deletes every user's star from the [messageId]. [Broker.notify]s stargazers via [messagesBroker].
     *
     * @see [deleteUserStar]
     * @see [deleteStars]
     */
    fun deleteStar(messageId: Int) {
        val stargazers = readStargazers(messageId)
        transaction {
            deleteWhere { Stargazers.messageId eq messageId }
        }
        for (stargazer in stargazers)
            messagesBroker.notify(
                update = { UpdatedMessage.build(it.userId, messageId) as MessagesSubscription },
                filter = { it.userId == stargazer }
            )
    }

    /**
     * [Broker.notify]s the [userId] that the [messageId] is no longer starred via [messagesBroker] unless not
     * [hasStar], in which case nothing will happen.
     *
     * @see [deleteStar]
     */
    fun deleteUserStar(userId: Int, messageId: Int) {
        if (!hasStar(userId, messageId)) return
        transaction {
            deleteWhere { (Stargazers.userId eq userId) and (Stargazers.messageId eq messageId) }
        }
        val message = UpdatedMessage.build(userId, messageId) as MessagesSubscription
        messagesBroker.notify(message) { it.userId == userId }
    }

    /**
     * Deletes every star from the [messageIdList].
     *
     * @see [deleteStar]
     */
    fun deleteStars(messageIdList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}