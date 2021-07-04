package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.chatsNotifier
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedPrivateChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnbookmarkedChat
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Stores when users delete their [PrivateChats].
 *
 * When a user deletes a chat, the chat is only deleted for themselves. The person they were chatting with still has the
 * chat in its original condition. If the person they were chatting with sends them a message, it will appear as the
 * first message in a new chat for the user.
 */
object PrivateChatDeletions : IntIdTable() {
    override val tableName = "private_chat_deletions"
    private val chatId: Column<Int> = integer("chat_id").references(PrivateChats.id)
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }
    private val userId: Column<Int> = integer("user_id").references(Users.id)

    /**
     * Deletes the [chatId] for the [userId].
     *
     * Commonly deleted [Messages] are deleted. The user's older [PrivateChatDeletions] are deleted. If both the users
     * have deleted the [chatId], and there has been no activity in the [chatId] after they've deleted it, the [chatId]
     * is deleted from [PrivateChats], [PrivateChatDeletions], [Messages], and [TypingStatuses].
     *
     * The [userId] will be notified of the [UnbookmarkedChat] via [messagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId].
     */
    fun create(chatId: Int, userId: Int) {
        require(isUserInChat(userId, chatId)) { "The user (ID: $userId) isn't in the chat (ID: $chatId)." }
        insert(chatId, userId)
        Bookmarks.deleteUserChat(userId, chatId)
        deleteUnusedChatData(chatId, userId)
        chatsNotifier.publish(DeletedPrivateChat(chatId), UserId(userId))
    }

    /** Records in the DB that the [userId] deleted the [chatId]. */
    private fun insert(chatId: Int, userId: Int): Unit = transaction {
        insert {
            it[this.chatId] = chatId
            it[this.userId] = userId
        }
    }

    /**
     * Commonly deleted [Messages] are deleted. The user's older [PrivateChatDeletions] are deleted. If both the users
     * have deleted the [chatId], and there has been no activity in the [chatId] after they've deleted it, the [chatId]
     * is [PrivateChats.delete]d.
     */
    private fun deleteUnusedChatData(chatId: Int, userId: Int) {
        deleteCommonlyDeletedMessages(chatId)
        deletePreviousDeletionRecords(chatId, userId)
        if (isChatDeleted(chatId)) PrivateChats.delete(chatId)
    }

    /**
     * Whether both the users have deleted the [chatId], and there has been no activity in the chat after they've
     * deleted it.
     */
    private fun isChatDeleted(chatId: Int): Boolean {
        val (user1Id, user2Id) = PrivateChats.readUserIdList(chatId).toList()
        return isDeleted(user1Id, chatId) && isDeleted(user2Id, chatId)
    }

    /** Deletes [Messages] deleted by both users. */
    private fun deleteCommonlyDeletedMessages(chatId: Int) {
        readLastChatDeletion(chatId)?.let { Messages.deleteChatUntil(chatId, it) }
    }

    /** Deletes every private chat deletion record the [userId] has in the [chatId] except for the latest one. */
    private fun deletePreviousDeletionRecords(chatId: Int, userId: Int): Unit = transaction {
        val idList = select((PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId))
            .orderBy(PrivateChatDeletions.id)
            .map { it[PrivateChatDeletions.id].value }
            .dropLast(1)
        deleteWhere { PrivateChatDeletions.id inList idList }
    }

    /** Returns the last [LocalDateTime] both users deleted the [chatId], if both of them have. */
    private fun readLastChatDeletion(chatId: Int): LocalDateTime? = transaction {
        val deletions = select(PrivateChatDeletions.chatId eq chatId)
        val userIdList = deletions.map { it[userId] }.toSet()
        if (userIdList.size < 2) return@transaction null
        val readDateTime = { index: Int ->
            deletions.last { it[userId] == userIdList.elementAt(index) }[dateTime]
        }
        listOf(readDateTime(0), readDateTime(1)).minOrNull()
    }

    /** Returns the last time the [userId] deleted the [chatId], if they ever did. */
    fun readLastDeletion(chatId: Int, userId: Int): LocalDateTime? = transaction {
        select((PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId))
            .lastOrNull()
            ?.get(dateTime)
    }

    /** Whether the [userId] has deleted the [chatId], and no messages were sent after that. */
    fun isDeleted(userId: Int, chatId: Int): Boolean = transaction {
        val deletions = select((PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId))
        if (deletions.empty()) return@transaction false
        val lastDeletion = deletions.last { it[PrivateChatDeletions.userId] == userId }
        !Messages.isExistingFrom(chatId, lastDeletion[dateTime])
    }

    /** Deletes every record of chat deletions for the [chatId]. */
    fun delete(chatId: Int): Unit = transaction {
        deleteWhere { PrivateChatDeletions.chatId eq chatId }
    }
}
