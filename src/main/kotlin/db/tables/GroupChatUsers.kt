package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.ExitedUsers
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.GroupChatId
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnstarredChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChat
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/** The users in [GroupChats]. */
object GroupChatUsers : Table() {
    override val tableName = "group_chat_users"
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val isAdmin: Column<Boolean> = bool("is_admin")

    private fun isUserInChat(userId: Int, chatId: Int): Boolean = transaction {
        select((groupChatId eq chatId) and (GroupChatUsers.userId eq userId)).empty().not()
    }

    /**
     * Makes the [userIdList] admins of the [chatId].
     *
     * If [shouldNotify], subscribers will receive the [UpdatedGroupChat] via [groupChatsNotifier] and
     * [groupChatMetadataNotifier]. An [IllegalArgumentException] will be thrown if any of the [userIdList] aren't in
     * the chat.
     */
    fun makeAdmins(chatId: Int, userIdList: Collection<Int>, shouldNotify: Boolean = true) {
        val invalidUsers = userIdList.filterNot { isUserInChat(it, chatId) }
        require(invalidUsers.isEmpty()) { "The users (IDs: $invalidUsers) aren't in the chat (ID: $chatId)." }
        transaction {
            update({ (groupChatId eq chatId) and (userId inList userIdList) }) { it[isAdmin] = true }
        }
        if (shouldNotify) {
            val update = UpdatedGroupChat(chatId, adminIdList = readAdminIdList(chatId).toList())
            groupChatsNotifier.publish(update, readUserIdList(chatId).map(::UserId))
            groupChatMetadataNotifier.publish(update, ChatId(chatId))
        }
    }

    /** Returns the ID of every user the [userId] has a chat with, excluding their own ID. */
    fun readFellowParticipantIdList(userId: Int): Set<Int> =
        readChatIdList(userId).flatMap(this::readUserIdList).toSet() - userId

    /** Whether the [userId] is an admin of the [chatId]. */
    fun isAdmin(userId: Int, chatId: Int): Boolean = transaction {
        select((groupChatId eq chatId) and (GroupChatUsers.userId eq userId))
            .firstOrNull()
            ?.get(isAdmin) ?: false
    }

    /** Returns the user IDs (sorted in ascending order) in the [chatId] as per the [pagination]. */
    @Suppress("DuplicatedCode")
    fun readUserIdList(chatId: Int, pagination: ForwardPagination? = null): LinkedHashSet<Int> {
        var op = groupChatId eq chatId
        pagination?.after?.let { op = op and (userId greater it) }
        return transaction {
            select(op)
                .orderBy(userId)
                .let { if (pagination?.first == null) it else it.limit(pagination.first) }
                .map { it[userId] }
                .toLinkedHashSet()
        }
    }

    fun readAdminIdList(chatId: Int): Set<Int> = transaction {
        select((groupChatId eq chatId) and (isAdmin eq true)).map { it[userId] }.toSet()
    }

    /**
     * Adds the [users] who aren't already in the [chatId].
     *
     * Notifies existing users of the [UpdatedGroupChat] via [groupChatsNotifier], and the [users] of the [GroupChatId]
     * via [groupChatsNotifier]. Subscribers will be updated of the [UpdatedGroupChat] via [groupChatMetadataNotifier].
     */
    fun addUsers(chatId: Int, users: Collection<Int>) {
        val newUserIdList = users.filterNot { isUserInChat(it, chatId) }.toSet()
        transaction {
            batchInsert(newUserIdList) {
                this[groupChatId] = chatId
                this[userId] = it
                this[isAdmin] = false
            }
        }
        groupChatsNotifier.publish(GroupChatId(chatId), newUserIdList.map(::UserId))
        val update = UpdatedGroupChat(chatId, newUserIdList = newUserIdList.toList())
        groupChatsNotifier.publish(update, readUserIdList(chatId).minus(newUserIdList).map(::UserId))
        groupChatMetadataNotifier.publish(update, ChatId(chatId))
    }

    fun addUsers(chatId: Int, vararg users: Int): Unit = addUsers(chatId, users.toList())

    /**
     * Nothing happens if the [userId] is already in the chat. If the [inviteCode] doesn't belong to an invitable chat,
     * then an [IllegalArgumentException] gets thrown. Otherwise, [addUsers] gets called.
     */
    fun addUserViaInviteCode(userId: Int, inviteCode: UUID) {
        val chatId = GroupChats.readChatIdFromInviteCode(inviteCode)
        requireNotNull(chatId) { "There's no invitable chat having the invite code $inviteCode." }
        if (!isUserInChat(userId, chatId)) addUsers(chatId, userId)
    }

    /**
     * Whether the [userIdList] can be removed from the [chatId]. Returns `false` if there would be users sans admins
     * left in the [chatId]. Users who aren't in the chat are ignored.
     */
    private fun canUsersLeave(chatId: Int, userIdList: Collection<Int>): Boolean {
        val existing = readUserIdList(chatId).toSet()
        val supplied = userIdList.filter { it in existing }.toSet()
        return supplied == existing || (existing - supplied).any { isAdmin(it, chatId) }
    }

    fun canUsersLeave(chatId: Int, vararg userIdList: Int): Boolean = canUsersLeave(chatId, userIdList.toSet())

    /**
     * Removes users in the [userIdList] from the [chatId]. Returns whether the chat was deleted.
     *
     * Users who aren't in the chat are ignored. If every user is removed, the [chatId] will be [GroupChats.delete]d.
     * An [IllegalArgumentException] will be thrown if not [canUsersLeave].
     *
     * Subscribers in the chat (including the [userIdList]) will be notified of the [ExitedUsers]s via
     * [groupChatsNotifier] and [groupChatMetadataNotifier]. Removed users will be notified of the [UnstarredChat] via
     * [messagesNotifier]. Clients who have subscribed to the [chatId] via [chatMessagesNotifier],
     * [chatOnlineStatusesNotifier], [chatAccountsNotifier], [groupChatMetadataNotifier], and
     * [chatTypingStatusesNotifier] will be unsubscribed if the chat gets deleted.
     */
    suspend fun removeUsers(chatId: Int, userIdList: Set<Int>): Boolean {
        require(canUsersLeave(chatId, userIdList)) {
            "The users ($userIdList) cannot leave because the chat needs an admin."
        }
        val originalIdList = readUserIdList(chatId)
        val removedIdList = originalIdList.intersect(userIdList).toList()
        transaction {
            deleteWhere { (groupChatId eq chatId) and (userId inList removedIdList) }
        }
        removedIdList.forEach { Stargazers.deleteUserChat(it, chatId) }
        val update = ExitedUsers(chatId, removedIdList)
        groupChatsNotifier.publish(update, originalIdList.map(::UserId))
        groupChatMetadataNotifier.publish(update, ChatId(chatId))
        if (readUserIdList(chatId).isEmpty()) {
            GroupChats.delete(chatId)
            setOf(
                chatMessagesNotifier,
                chatOnlineStatusesNotifier,
                chatTypingStatusesNotifier,
                chatAccountsNotifier,
                groupChatMetadataNotifier,
            ).forEach { notifier ->
                notifier.unsubscribe { it.data.chatId == chatId }
            }
            return true
        }
        return false
    }

    suspend fun removeUsers(chatId: Int, vararg userIdList: Int): Boolean = removeUsers(chatId, userIdList.toSet())

    /**
     * Whether the [userId] can leave every chat they're in. Returns `false` only if they're the last admin of a chat
     * with other users in it.
     */
    fun canUserLeave(userId: Int): Boolean = readChatIdList(userId).all { canUsersLeave(it, userId) }

    /** Calls [removeUsers] on the [userId] for every chat they're in. The [userId] needn't exist. */
    suspend fun removeUser(userId: Int): Unit = readChatIdList(userId).forEach { removeUsers(it, userId) }

    /**
     * Returns the chat IDs of every chat the [userId] is in, or an empty [LinkedHashSet] if the [userId] doesn't exist.
     * The chat IDs are sorted in ascending order.
     */
    fun readChatIdList(userId: Int): LinkedHashSet<Int> = transaction {
        select(GroupChatUsers.userId eq userId).orderBy(groupChatId).map { it[groupChatId] }.toLinkedHashSet()
    }
}
