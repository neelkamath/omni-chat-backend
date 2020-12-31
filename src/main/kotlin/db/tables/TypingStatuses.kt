package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.readUserIdList
import com.neelkamath.omniChat.db.typingStatusesNotifier
import com.neelkamath.omniChat.graphql.routing.TypingStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** Whether the user is typing in a chat. */
object TypingStatuses : Table() {
    override val tableName = "typing_statuses"
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val isTyping: Column<Boolean> = bool("is_typing")

    /** Notifies subscribers of the [TypingStatus] via [typingStatusesNotifier]. */
    fun set(chatId: Int, userId: Int, isTyping: Boolean) {
        if (exists(chatId, userId)) update(chatId, userId, isTyping) else insert(chatId, userId, isTyping)
        val subscribers = readUserIdList(chatId).minus(userId)
        typingStatusesNotifier.publish(TypingStatus(chatId, userId, isTyping), subscribers)
    }

    /** Whether the [userId] has a record in this table for the [chatId]. */
    private fun exists(chatId: Int, userId: Int): Boolean = transaction {
        !select { (TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId) }.empty()
    }

    /** Updates the existing record in the table. */
    private fun update(chatId: Int, userId: Int, isTyping: Boolean): Unit = transaction {
        update({ (TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId) }) {
            it[this.isTyping] = isTyping
        }
    }

    /** Inserts the record into the table. */
    private fun insert(chatId: Int, userId: Int, isTyping: Boolean): Unit = transaction {
        insert {
            it[this.chatId] = chatId
            it[this.userId] = userId
            it[this.isTyping] = isTyping
        }
    }

    /** Whether the [userId] is typing in the [chatId]. */
    fun read(chatId: Int, userId: Int): Boolean = transaction {
        select { (TypingStatuses.chatId eq chatId) and (TypingStatuses.userId eq userId) }
                .firstOrNull()
                ?.get(isTyping) ?: false
    }

    /** Deletes every status created on the [chatId]. */
    fun deleteChat(chatId: Int): Unit = transaction {
        deleteWhere { TypingStatuses.chatId eq chatId }
    }

    /** Deletes every status created by the [userId], if the [userId] exists. */
    fun deleteUser(userId: Int): Unit = transaction {
        deleteWhere { TypingStatuses.userId eq userId }
    }
}
