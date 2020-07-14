package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.*

/** Chats users are typing in. */
object TypingStatuses : Table() {
    override val tableName = "typing_statuses"
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val isTyping: Column<Boolean> = bool("is_typing")

    fun set(userId: Int, chatId: Int, isTyping: Boolean) =
        if (exists(userId, chatId)) update(userId, chatId, isTyping) else insert(userId, chatId, isTyping)

    /** Whether the [userId] has a record in this table for the [chatId]. */
    private fun exists(userId: Int, chatId: Int): Boolean = transact {
        !select { (TypingStatuses.userId eq userId) and (TypingStatuses.chatId eq chatId) }.empty()
    }

    private fun update(userId: Int, chatId: Int, isTyping: Boolean): Unit = transact {
        update({ (TypingStatuses.userId eq userId) and (TypingStatuses.chatId eq chatId) }) {
            it[this.isTyping] = isTyping
        }
    }

    /** Inserts the record into the table. */
    private fun insert(userId: Int, chatId: Int, isTyping: Boolean): Unit = transact {
        insert {
            it[TypingStatuses.userId] = userId
            it[TypingStatuses.chatId] = chatId
            it[TypingStatuses.isTyping] = isTyping
        }
    }

    /** Whether the [userId] is typing in the [chatId]. */
    fun read(userId: Int, chatId: Int): Boolean = transact {
        select { (TypingStatuses.userId eq userId) and (TypingStatuses.chatId eq chatId) }
            .firstOrNull()
            ?.get(isTyping) ?: false
    }

    /** Deletes every status created on the [chatId]. */
    fun deleteChat(chatId: Int): Unit = transact {
        deleteWhere { TypingStatuses.chatId eq chatId }
    }

    /** Deletes every status created by the [userId]. */
    fun deleteUser(userId: Int): Unit = transact {
        deleteWhere { TypingStatuses.userId eq userId }
    }
}