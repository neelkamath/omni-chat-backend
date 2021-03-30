package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.graphql.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
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

    fun read(userId: Int, pagination: ForwardPagination? = null): StarredMessagesConnection {
        var op = Stargazers.userId eq userId
        if (pagination?.after != null) op = op and (messageId greater pagination.after)
        val edges = transaction {
            select(op)
                .orderBy(messageId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { StarredMessageEdge(StarredMessage.build(userId, it[messageId]), cursor = it[messageId]) }
        }
        return StarredMessagesConnection(edges, buildPageInfo(userId, edges))
    }

    private fun buildPageInfo(userId: Int, edges: List<StarredMessageEdge>): PageInfo = transaction {
        val startCursor = if (edges.isEmpty()) null else edges[0].cursor
        val endCursor = if (edges.isEmpty()) null else edges.last().cursor
        val hasPage = { filter: Op<Boolean> -> select((Stargazers.userId eq userId) and filter).empty().not() }
        PageInfo(
            hasNextPage = if (endCursor == null) false else hasPage(messageId greater endCursor),
            hasPreviousPage = if (startCursor == null) false else hasPage(messageId less startCursor),
            startCursor,
            endCursor,
        )
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
