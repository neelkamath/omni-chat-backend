package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.StarredMessageEdge
import com.neelkamath.omniChatBackend.graphql.routing.UnstarredChat
import com.neelkamath.omniChatBackend.graphql.routing.UpdatedMessage
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction

/** Users' starred [Messages]. */
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
        val message = Messages.readMessage(userId, messageId).toUpdatedMessage()
        messagesNotifier.publish(message, userId)
    }

    /** Returns the message IDs the [userId] starred as per the [pagination]. */
    fun readMessageIdList(userId: Int, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = Stargazers.userId eq userId
        if (pagination?.after != null) op = op and (messageId greater pagination.after)
        return transaction {
            select(op)
                .orderBy(messageId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[messageId] }
                .toLinkedHashSet()
        }
    }

    /** The [lastMessageId] is the ID of the last [StarredMessageEdge] read by the [pagination] for the [userId]. */
    fun readPageInfo(userId: Int, lastMessageId: Int?, pagination: ForwardPagination? = null): PageInfo {
        val startCursor = readCursor(userId, CursorType.START)
        val endCursor = readCursor(userId, CursorType.END)
        val hasNextPage = when {
            endCursor == null -> false
            lastMessageId != null -> lastMessageId < endCursor
            pagination?.after == null -> true
            else -> pagination.after < endCursor
        }
        val hasPreviousPage =
            if (startCursor == null || pagination?.after == null) false else startCursor <= pagination.after
        return PageInfo(hasNextPage, hasPreviousPage, startCursor, endCursor)
    }

    /** Reads the [type] of cursor for the [userId]. Returns `null` if the user hasn't starred any messages. */
    private fun readCursor(userId: Int, type: CursorType): Int? {
        val order = when (type) {
            CursorType.END -> SortOrder.DESC
            CursorType.START -> SortOrder.ASC
        }
        return transaction {
            select(Stargazers.userId eq userId).orderBy(messageId, order).limit(1).firstOrNull()?.get(messageId)
        }
    }

    /** Returns the ID of every user who has starred the [messageId]. */
    private fun readStargazers(messageId: Int): Set<Int> = transaction {
        select(Stargazers.messageId eq messageId).map { it[userId] }.toSet()
    }

    fun hasStar(userId: Int, messageId: Int): Boolean =
        transaction { select((Stargazers.userId eq userId) and (Stargazers.messageId eq messageId)).empty().not() }

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
            val notification = Messages.readMessage(it, messageId).toUpdatedMessage()
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
        messagesNotifier.publish(Messages.readMessage(userId, messageId).toUpdatedMessage(), userId)
    }

    /**
     * Deletes every star from the [messageIdList].
     *
     * @see [deleteStar]
     * @see [deleteUserChat]
     */
    fun deleteStars(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /**
     * Unstars every message the [userId] starred in the [chatId]. Notifies the [userId] of the [UnstarredChat] via
     * [messagesNotifier]. It's assumed the [userId] is in the [chatId].
     */
    fun deleteUserChat(userId: Int, chatId: Int) {
        transaction {
            deleteWhere { (Stargazers.userId eq userId) and (messageId inList Messages.readIdList(chatId)) }
        }
        messagesNotifier.publish(UnstarredChat(chatId), userId)
    }
}
