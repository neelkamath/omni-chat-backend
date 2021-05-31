package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.readUserIdList
import com.neelkamath.omniChatBackend.db.typingStatusesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TypingStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** Which users are typing in which chats. */
object TypingStatuses : Table() {
    override val tableName = "typing_statuses"
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val userId: Column<Int> = integer("user_id").references(Users.id)

    /** Notifies subscribers of the [TypingStatus] via [typingStatusesNotifier] if necessary. */
    fun update(chatId: Int, userId: Int, isTyping: Boolean) {
        when {
            isExisting(chatId, userId) && isTyping -> return
            isExisting(chatId, userId) && !isTyping -> deleteRecord(chatId, userId)
            !isExisting(chatId, userId) && isTyping -> insert(chatId, userId)
            !isExisting(chatId, userId) && !isTyping -> return
        }
        typingStatusesNotifier.publish(TypingStatus(chatId, userId), readUserIdList(chatId).map(::UserId))
    }

    /** Whether the [userId] has a record in this table for the [chatId]. */
    private fun isExisting(chatId: Int, userId: Int): Boolean =
        transaction { select((TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId)).empty().not() }

    /** Deletes the specified record if it exists. This means the [userId] is no longer typing in the [chatId]. */
    private fun deleteRecord(chatId: Int, userId: Int): Unit = transaction {
        deleteWhere { (TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId) }
    }

    /** Inserts the specified record. This means the [userId] is typing in the [chatId]. */
    private fun insert(chatId: Int, userId: Int): Unit = transaction {
        insert {
            it[this.chatId] = chatId
            it[this.userId] = userId
        }
    }

    /** Whether the [userId] is typing in the [chatId]. */
    fun isTyping(chatId: Int, userId: Int): Boolean =
        transaction { select((TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId)).empty().not() }

    /** Returns the IDs of users who are typing in the [chatId]. */
    fun readChat(chatId: Int): Set<Int> =
        transaction { select(TypingStatuses.chatId eq chatId).map { it[userId] }.toSet() }

    /** Deletes every status created on the [chatId]. */
    fun deleteChat(chatId: Int): Unit = transaction {
        deleteWhere { TypingStatuses.chatId eq chatId }
    }

    /** Deletes every status created by the [userId], if the [userId] exists. */
    fun deleteUser(userId: Int): Unit = transaction {
        deleteWhere { TypingStatuses.userId eq userId }
    }
}
