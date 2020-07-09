package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DeletionOfEveryMessage
import com.neelkamath.omniChat.PrivateChat
import com.neelkamath.omniChat.USER_ID_LENGTH
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.readUserById
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * [PrivateChats] have [Messages]. [PrivateChatDeletions] are when each user deleted their chat. If both users have
 * deleted the chat, then the chat's record will be deleted.
 */
object PrivateChats : Table() {
    override val tableName get() = "private_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val user1Id: Column<String> = varchar("user_1_id", USER_ID_LENGTH)
    private val user2Id: Column<String> = varchar("user_2_id", USER_ID_LENGTH)

    /**
     * @return the created chat's ID.
     * @throws [IllegalArgumentException] if the chat exists.
     */
    fun create(user1Id: String, user2Id: String): Int {
        if (exists(user1Id, user2Id))
            throw IllegalArgumentException("The chat between user 1 (ID: $user1Id) and user 2 (ID: $user2Id) exists.")
        return insert(user1Id, user2Id)
    }

    /** Records in the DB that [user1Id] and [user2Id] are in a chat with each other, and returns the chat's ID. */
    private fun insert(user1Id: String, user2Id: String): Int = transact {
        insert {
            it[id] = Chats.create()
            it[PrivateChats.user1Id] = user1Id
            it[PrivateChats.user2Id] = user2Id
        }[PrivateChats.id]
    }

    /**
     * @return the ID of the chat between the [participantId] (is in the chat) and [userId] (may be in the chat). You
     * can check if the [PrivateChats.exists].
     */
    fun readChatId(participantId: String, userId: String): Int =
        readUserChats(participantId, BackwardPagination(last = 0)).first { it.user.id == userId }.id

    /**
     * @return the [userId]'s chats. Chats the [userId] deleted, which had no activity after their deletion, aren't
     * returned.
     * @see [readIdList]
     * @see [readUserChatIdList]
     */
    fun readUserChats(userId: String, pagination: BackwardPagination? = null): List<PrivateChat> =
        readUserChatsRows(userId).map { buildPrivateChat(it, userId, pagination) }

    /**
     * @return the [userId]'s chats. Chats the [userId] deleted, which had no activity after their deletion, aren't
     * returned.
     * @see [readIdList]
     * @see [readUserChats]
     */
    fun readUserChatIdList(userId: String): List<Int> = readUserChatsRows(userId).map { it[id] }

    /**
     * @return every chat the [userId] is in, excluding ones they've deleted which have had no activity after their
     * deletion.
     */
    private fun readUserChatsRows(userId: String): List<ResultRow> = transact {
        select { (user1Id eq userId) or (user2Id eq userId) }
            .filterNot { PrivateChatDeletions.isDeleted(userId, it[PrivateChats.id]) }
    }

    fun read(id: Int, userId: String, pagination: BackwardPagination? = null): PrivateChat = transact {
        select { PrivateChats.id eq id }.first()
    }.let { buildPrivateChat(it, userId, pagination) }

    private fun buildPrivateChat(
        row: ResultRow,
        userId: String,
        pagination: BackwardPagination? = null
    ): PrivateChat {
        val otherUserId = if (row[user1Id] == userId) row[user2Id] else row[user1Id]
        return PrivateChat(
            row[id],
            readUserById(otherUserId),
            Messages.readPrivateChatConnection(row[id], userId, pagination)
        )
    }

    /**
     * @return the ID list of the [userId]'s chats, including deleted chat IDs.
     * @see [readUserChats]
     */
    fun readIdList(userId: String): List<Int> = transact {
        select { (user1Id eq userId) or (user2Id eq userId) }.map { it[PrivateChats.id] }
    }

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in, excluding ones the [userId] deleted.
     * Only chats having messages matching the [query] will be returned. Only the matched message [ChatEdges.edges] will
     * be returned.
     */
    fun queryUserChatEdges(userId: String, query: String): List<ChatEdges> = readUserChatIdList(userId)
        .associateWith { Messages.searchPrivateChat(it, userId, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }

    /**
     * Whether [user1Id] and [user2Id] are in a chat with each other (i.e., a chat [PrivateChats.exists] between them,
     * and neither of them has the chat deleted).
     */
    fun areInChat(user1Id: String, user2Id: String): Boolean {
        val hasChatWith = { firstUserId: String, secondUserId: String ->
            readUserChats(firstUserId, BackwardPagination(last = 0)).any { it.user.id == secondUserId }
        }
        return hasChatWith(user1Id, user2Id) && hasChatWith(user2Id, user1Id)
    }

    /**
     * Whether there exists a chat between [user1Id] and [user2Id].
     *
     * @see [areInChat]
     */
    fun exists(user1Id: String, user2Id: String): Boolean = transact {
        val where = { userId: String -> (PrivateChats.user1Id eq userId) or (PrivateChats.user2Id eq userId) }
        !select { where(user1Id) and where(user2Id) }.empty()
    }

    /**
     * Searches chats the [userId] has by case-insensitively [query]ing other users' first name, last name, and
     * username. Chats the [userId] deleted, which had no activity after their deletion, are not searched.
     */
    fun search(userId: String, query: String, pagination: BackwardPagination? = null): List<PrivateChat> =
        readUserChats(userId, pagination).filter { readUserById(it.user.id).matches(query) }

    /**
     * Deletes every record the [userId] has in [PrivateChats], [PrivateChatDeletions], [Messages], and
     * [MessageStatuses]. Chats the [userId] deleted, which had no activity after their deletion, are deleted as well.
     * Clients will be notified of a [DeletionOfEveryMessage], and then [Broker.unsubscribe]d via
     * [messagesBroker].
     */
    fun deleteUserChats(userId: String) {
        val chatIdList = transact {
            select { (user1Id eq userId) or (user2Id eq userId) }.map { it[PrivateChats.id] }
        }
        chatIdList.forEach {
            Messages.deleteChat(it)
            PrivateChatDeletions.delete(it)
        }
        transact {
            deleteWhere { PrivateChats.id inList chatIdList }
        }
    }

    /** Deletes the [chatId] from [PrivateChats]. */
    fun delete(chatId: Int): Unit = transact {
        deleteWhere { PrivateChats.id eq chatId }
    }

    /**
     * @return the IDs of the users in the [chatId]. Even if one of the users has deleted the chat, their ID will be
     * returned.
     * @see [readOtherUserId]
     */
    fun readUserIdList(chatId: Int): List<String> = transact {
        val row = select { PrivateChats.id eq chatId }.first()
        listOf(row[user1Id], row[user2Id])
    }

    /**
     * @return the ID of the other user in the [chatId].
     * @see [readUserIdList]
     */
    fun readOtherUserId(chatId: Int, userId: String): String {
        val userIdList = readUserIdList(chatId)
        return if (userIdList[0] == userId) userIdList[1] else userIdList[0]
    }

    /** @return the ID of every other user the [userId] has chats with, including deleted chats. */
    fun readOtherUserIdList(userId: String): List<String> = readIdList(userId).map { readOtherUserId(it, userId) }
}