package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.graphql.routing.MessagesSubscription
import com.neelkamath.omniChat.graphql.routing.UpdatedMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** Users who have starred [Messages]. */
object Stargazers : Table() {
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)

    /**
     * If [hasStar], nothing happens. Otherwise, the [userId] is notified of the starred [messageId] via
     * [messagesNotifier].
     */
    fun create(userId: Int, messageId: Int) {
        if (hasStar(userId, messageId)) return
        transaction {
            insert {
                it[this.userId] = userId
                it[this.messageId] = messageId
            }
        }
        val message = UpdatedMessage.build(userId, messageId) as MessagesSubscription
        messagesNotifier.publish(message, userId)
    }

    /** Returns the ID of every message the [userId] has starred. */
    fun read(userId: Int): Set<Int> = transaction {
        select { Stargazers.userId eq userId }.map { it[messageId] }.toSet()
    }

    /** Returns the ID of every user who has starred the [messageId]. */
    private fun readStargazers(messageId: Int): Set<Int> = transaction {
        select { Stargazers.messageId eq messageId }.map { it[userId] }.toSet()
    }

    fun hasStar(userId: Int, messageId: Int): Boolean = transaction {
        select { (Stargazers.userId eq userId) and (Stargazers.messageId eq messageId) }.empty().not()
    }

    /**
     * Deletes every user's star from the [messageId]. Notifies stargazers of the [UpdatedMessage] via
     * [messagesNotifier].
     *
     * @see [deleteUserStar]
     * @see [deleteStars]
     */
    fun deleteStar(messageId: Int) {
        val stargazers = readStargazers(messageId)
        transaction {
            deleteWhere { Stargazers.messageId eq messageId }
        }
        stargazers.forEach {
            val notification = UpdatedMessage.build(it, messageId) as MessagesSubscription
            messagesNotifier.publish(it to notification)
        }
    }

    /**
     * If not [hasStar], nothing will happen. Otherwise, the [userId] will be notified of the [UpdatedMessage] via
     * [messagesNotifier].
     *
     * @see [deleteStar]
     */
    fun deleteUserStar(userId: Int, messageId: Int) {
        if (!hasStar(userId, messageId)) return
        transaction {
            deleteWhere { (Stargazers.userId eq userId) and (Stargazers.messageId eq messageId) }
        }
        messagesNotifier.publish(UpdatedMessage.build(userId, messageId) as MessagesSubscription, userId)
    }

    /**
     * Deletes every star from the [messageIdList].
     *
     * @see [deleteStar]
     */
    fun deleteStars(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }
}
