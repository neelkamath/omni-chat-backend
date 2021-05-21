package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.db.readUserIdList
import com.neelkamath.omniChatBackend.linkedHashSetOf
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @see Messages
 * @see PrivateChatDeletions
 */
object PrivateChats : Table() {
    override val tableName = "private_chats"
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
     * Returns the ID of the chat between the [user1Id] and [user2Id].
     *
     * @see PrivateChats.isExisting
     */
    fun readChatId(user1Id: Int, user2Id: Int): Int {
        val list = listOf(user1Id, user2Id)
        return transaction {
            select((PrivateChats.user1Id inList list) and (PrivateChats.user2Id inList list)).first()[PrivateChats.id]
        }
    }

    /**
     * Returns the chat IDs (sorted in ascending order) the [userId] is in. Chats the [userId] deleted, which had no
     * activity after their deletion, aren't returned.
     *
     * @see readIdList
     */
    fun readUserChatIdList(userId: Int): LinkedHashSet<Int> = transaction {
        select((user1Id eq userId) or (user2Id eq userId))
            .orderBy(PrivateChats.id)
            .fold(linkedHashSetOf()) { chatIdList, row ->
                if (!PrivateChatDeletions.isDeleted(userId, row[PrivateChats.id])) chatIdList.add(row[PrivateChats.id])
                chatIdList
            }
    }

    /** Returns the ID list of the [userId]'s chats. This includes chats which had no new messages post-deletion. */
    fun readIdList(userId: Int): Set<Int> =
        transaction { select((user1Id eq userId) or (user2Id eq userId)).map { it[PrivateChats.id] }.toSet() }

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in, excluding ones the [userId] deleted.
     *
     * Only chats having messages matching the [query] will be returned, and they'll only contain the matched
     * [ChatEdges.messageIdList]. The returned [ChatEdges] are sorted in ascending order of their [ChatEdges.chatId].
     */
    fun queryUserChatEdges(userId: Int, query: String): LinkedHashSet<ChatEdges> = readUserChatIdList(userId)
        .associateWith { Messages.searchPrivateChat(it, userId, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }
        .toLinkedHashSet()

    /** Whether there exists a chat between [user1Id] and [user2Id]. */
    fun isExisting(user1Id: Int, user2Id: Int): Boolean = transaction {
        val where = { userId: Int -> (PrivateChats.user1Id eq userId) or (PrivateChats.user2Id eq userId) }
        select(where(user1Id) and where(user2Id)).empty().not()
    }

    /**
     * Searches chats the [userId] has by case-insensitively [query]ing other users' usernames, first names, last names,
     * and email addresses. Returns the IDs of each matched chat in ascending order.
     *
     * Chats the [userId] deleted, which had no activity after their deletion, aren't searched.
     */
    fun search(userId: Int, query: String): LinkedHashSet<Int> = readOtherUserIdList(userId)
        .filter { otherUserId ->
            listOf(
                Users.readUsername(otherUserId).value,
                Users.readFirstName(otherUserId).value,
                Users.readLastName(otherUserId).value,
                Users.readEmailAddress(otherUserId),
            ).any { it.contains(query, ignoreCase = true) }
        }
        .map { readChatId(userId, it) }
        .toLinkedHashSet()

    /** [delete]s every chat which the [userId] is in. Nothing will happen if the [userId] doesn't exist. */
    fun deleteUserChats(userId: Int): Unit = readIdList(userId).forEach(::delete)

    /**
     * Deletes the [chatId].
     *
     * @see deleteUserChats
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
     * @see readOtherUserId
     */
    fun readUserIdList(chatId: Int): Set<Int> = transaction {
        val row = select(PrivateChats.id eq chatId).first()
        setOf(row[user1Id], row[user2Id])
    }

    /**
     * Returns the ID of the other user in the [chatId].
     *
     * @see readUserIdList
     */
    fun readOtherUserId(chatId: Int, userId: Int): Int {
        val (user1Id, user2Id) = readUserIdList(chatId).toList()
        return if (userId == user1Id) user2Id else user1Id
    }

    /**
     * Returns the user IDs (sorted in ascending order) the [userId] has chats with. Chats the userId deleted, which had
     * no activity after their deletion, aren't returned.
     */
    fun readOtherUserIdList(userId: Int): LinkedHashSet<Int> =
        readUserChatIdList(userId).map { readOtherUserId(it, userId) }.toLinkedHashSet()
}
