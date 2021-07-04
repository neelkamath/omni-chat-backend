package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnbookmarkedChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedMessage
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction

/** Users' bookmarked [Messages]. */
object Bookmarks : Table() {
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)

    /**
     * If [isBookmarked], nothing happens. Otherwise, the [userId] is notified of the bookmarked [messageId] via
     * [messagesNotifier].
     */
    fun create(userId: Int, messageId: Int) {
        if (isBookmarked(userId, messageId)) return
        transaction {
            insert {
                it[this.userId] = userId
                it[this.messageId] = messageId
            }
        }
        messagesNotifier.publish(UpdatedMessage(messageId), UserId(userId))
    }

    /** Returns the IDs (sorted in ascending order) of messages the [userId] bookmarked as per the [pagination]. */
    @Suppress("DuplicatedCode")
    fun readMessageIdList(userId: Int, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = Bookmarks.userId eq userId
        pagination?.after?.let { op = op and (messageId greater it) }
        return transaction {
            select(op)
                .orderBy(messageId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[messageId] }
                .toLinkedHashSet()
        }
    }

    /** Returns the [type] of cursor for the [userId]. Returns `null` if the user hasn't bookmarked any messages. */
    fun readCursor(userId: Int, type: CursorType): Int? {
        val order = when (type) {
            CursorType.END -> SortOrder.DESC
            CursorType.START -> SortOrder.ASC
        }
        return transaction {
            select(Bookmarks.userId eq userId)
                .orderBy(messageId, order)
                .limit(1)
                .firstOrNull()
                ?.get(messageId)
        }
    }

    /** Returns the ID of every user who has bookmarked the [messageId]. */
    private fun readBookmarks(messageId: Int): Set<Int> = transaction {
        select(Bookmarks.messageId eq messageId).map { it[userId] }.toSet()
    }

    fun isBookmarked(userId: Int, messageId: Int): Boolean =
        transaction { select((Bookmarks.userId eq userId) and (Bookmarks.messageId eq messageId)).empty().not() }

    /**
     * Deletes every user's bookmark from the [messageId]. Notifies users who have bookmarked the [messageId] of the
     * [UpdatedMessage] via [messagesNotifier].
     *
     * @see deleteUserBookmark
     * @see deleteBookmarks
     */
    fun deleteBookmark(messageId: Int) {
        val bookmakers = readBookmarks(messageId)
        transaction {
            deleteWhere { Bookmarks.messageId eq messageId }
        }
        bookmakers.forEach { messagesNotifier.publish(UpdatedMessage(messageId), UserId(it)) }
    }

    /**
     * Nothing will happen if either the message doesn't exist or it isn't bookmarked. Otherwise, the [userId] will be
     * notified of the [UpdatedMessage] via [messagesNotifier].
     *
     * @see deleteBookmark
     */
    fun deleteUserBookmark(userId: Int, messageId: Int) {
        if (!isBookmarked(userId, messageId)) return
        transaction {
            deleteWhere { (Bookmarks.userId eq userId) and (Bookmarks.messageId eq messageId) }
        }
        messagesNotifier.publish(UpdatedMessage(messageId), UserId(userId))
    }

    /**
     * Deletes every bookmark from the [messageIdList].
     *
     * @see deleteBookmark
     * @see deleteUserChat
     */
    fun deleteBookmarks(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /**
     * Deletes the bookmark for every message the [userId] bookmarked in the [chatId]. Notifies the [userId] of the
     * [UnbookmarkedChat] via [messagesNotifier]. It's assumed the [userId] is in the [chatId].
     */
    fun deleteUserChat(userId: Int, chatId: Int) {
        transaction {
            deleteWhere { (Bookmarks.userId eq userId) and (messageId inList Messages.readIdList(chatId)) }
        }
        messagesNotifier.publish(UnbookmarkedChat(chatId), UserId(userId))
    }
}
