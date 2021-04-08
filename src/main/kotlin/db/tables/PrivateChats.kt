package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.db.Notifier
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.routing.DeletionOfEveryMessage
import com.neelkamath.omniChatBackend.graphql.routing.PrivateChat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @see [Messages]
 * @see [PrivateChatDeletions]
 */
object PrivateChats : Table() {
    override val tableName get() = "private_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val user1Id: Column<Int> = integer("user_1_id").references(Users.id)
    private val user2Id: Column<Int> = integer("user_2_id").references(Users.id)

    /**
     * Returns the created chat's ID.
     *
     * An [IllegalArgumentException] will be thrown if the chat exists.
     */
    fun create(user1Id: Int, user2Id: Int): Int {
        require(!isExisting(user1Id, user2Id)) {
            "The chat between user 1 (ID: $user1Id) and user 2 (ID: $user2Id) exists."
        }
        return insert(user1Id, user2Id)
    }

    fun isExisting(chatId: Int): Boolean = transaction { select(PrivateChats.id eq chatId).empty().not() }

    /** Records in the DB that [user1Id] and [user2Id] are in a chat with each other, and returns the chat's ID. */
    private fun insert(user1Id: Int, user2Id: Int): Int = transaction {
        insert {
            it[id] = Chats.create()
            it[this.user1Id] = user1Id
            it[this.user2Id] = user2Id
        }[PrivateChats.id]
    }

    /**
     * Returns the ID of the chat between the [participantId] (is in the chat) and [userId] (may be in the chat). You
     * can check if the [PrivateChats.isExisting].
     */
    fun readChatId(participantId: Int, userId: Int): Int =
        readUserChats(participantId, BackwardPagination(last = 0)).first { it.user.id == userId }.id

    /**
     * Returns the [userId]'s chats. Chats the [userId] deleted, which had no activity after their deletion, aren't
     * returned.
     *
     * @see [readIdList]
     * @see [readUserChatIdList]
     */
    fun readUserChats(userId: Int, messagesPagination: BackwardPagination? = null): Set<PrivateChat> =
        readUserChatsRows(userId).map { buildPrivateChat(it, userId, messagesPagination) }.toSet()

    /**
     * Returns the [userId]'s chats. Chats the [userId] deleted, which had no activity after their deletion, aren't
     * returned.
     *
     * @see [readIdList]
     * @see [readUserChats]
     */
    fun readUserChatIdList(userId: Int): Set<Int> = readUserChatsRows(userId).map { it[id] }.toSet()

    /**
     * Returns every chat the [userId] is in, excluding ones they've deleted which have had no activity after their
     * deletion.
     */
    private fun readUserChatsRows(userId: Int): Set<ResultRow> = transaction {
        select((user1Id eq userId) or (user2Id eq userId))
            .filterNot { PrivateChatDeletions.isDeleted(userId, it[PrivateChats.id]) }
            .toSet()
    }

    fun read(id: Int, userId: Int, pagination: BackwardPagination? = null): PrivateChat {
        val row = transaction { select(PrivateChats.id eq id).first() }
        return buildPrivateChat(row, userId, pagination)
    }

    private fun buildPrivateChat(
        row: ResultRow,
        userId: Int,
        pagination: BackwardPagination? = null,
    ): PrivateChat {
        val otherUserId = if (row[user1Id] == userId) row[user2Id] else row[user1Id]
        return PrivateChat(
            row[id],
            Users.read(otherUserId).toAccount(),
            Messages.readPrivateChatConnection(row[id], userId, pagination),
        )
    }

    /**
     * Returns the ID list of the [userId]'s chats, including deleted chat IDs.
     *
     * @see [readUserChats]
     */
    fun readIdList(userId: Int): Set<Int> =
        transaction { select((user1Id eq userId) or (user2Id eq userId)).map { it[PrivateChats.id] }.toSet() }

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in, excluding ones the [userId] deleted.
     * Only chats having messages matching the [query] will be returned. Only the matched message [ChatEdges.edges] will
     * be returned.
     */
    fun queryUserChatEdges(userId: Int, query: String): Set<ChatEdges> = readUserChatIdList(userId)
        .associateWith { Messages.searchPrivateChat(it, userId, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }
        .toSet()

    /** Whether there exists a chat between [user1Id] and [user2Id]. */
    fun isExisting(user1Id: Int, user2Id: Int): Boolean = transaction {
        val where = { userId: Int -> (PrivateChats.user1Id eq userId) or (PrivateChats.user2Id eq userId) }
        select(where(user1Id) and where(user2Id)).empty().not()
    }

    /**
     * Searches chats the [userId] has by case-insensitively [query]ing other users' first name, last name, and
     * username. Chats the [userId] deleted, which had no activity after their deletion, are not searched.
     */
    fun search(userId: Int, query: String, pagination: BackwardPagination? = null): Set<PrivateChat> =
        readUserChats(userId, pagination).filter { Users.read(it.user.id).toAccount().matches(query) }.toSet()

    /** [delete]s every chat which the [userId] is in. Nothing will happen if the [userId] doesn't exist. */
    fun deleteUserChats(userId: Int): Unit = readIdList(userId).forEach(::delete)

    /**
     * Deletes the [chatId].
     *
     * Clients will be notified of a [DeletionOfEveryMessage], and then [Notifier.unsubscribe]d via [messagesNotifier].
     *
     * @see [deleteUserChats]
     */
    fun delete(chatId: Int) {
        Messages.deleteChat(chatId)
        TypingStatuses.deleteChat(chatId)
        PrivateChatDeletions.delete(chatId)
        transaction {
            deleteWhere { PrivateChats.id eq chatId }
        }
        Chats.delete(chatId)
    }

    /**
     * Returns the two IDs of the users in the [chatId]. Even if one of the users has deleted the chat, their ID will be
     * returned.
     *
     * @see [readOtherUserId]
     */
    fun readUserIdList(chatId: Int): Set<Int> = transaction {
        val row = select(PrivateChats.id eq chatId).first()
        listOf(row[user1Id], row[user2Id]).toSet()
    }

    /**
     * Returns the ID of the other user in the [chatId].
     *
     * @see [readUserIdList]
     */
    fun readOtherUserId(chatId: Int, userId: Int): Int {
        val (user1Id, user2Id) = readUserIdList(chatId).toList()
        return if (userId == user1Id) user2Id else user1Id
    }

    /** Returns the ID of every other user the [userId] has chats with (excluding deleted chats). */
    fun readOtherUserIdList(userId: Int): Set<Int> =
        readUserChats(userId, messagesPagination = BackwardPagination(last = 0)).map { it.user.id }.toSet()
}
