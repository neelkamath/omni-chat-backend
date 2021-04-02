package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.LikeOp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase
import org.postgresql.util.PGobject

val db: Database by lazy {
    val url = System.getenv("POSTGRES_URL")
    val db = System.getenv("POSTGRES_DB")
    Database.connect(
        "jdbc:postgresql://$url/$db?reWriteBatchedInserts=true",
        "org.postgresql.Driver",
        System.getenv("POSTGRES_USER"),
        System.getenv("POSTGRES_PASSWORD"),
    )
}

data class ChatEdges(
    val chatId: Int,
    /** Chronologically ordered. */
    val edges: LinkedHashSet<MessageEdge>,
)

data class ForwardPagination(val first: Int? = null, val after: Cursor? = null)

data class BackwardPagination(val last: Int? = null, val before: Cursor? = null)

enum class MessageType { TEXT, ACTION, PIC, AUDIO, VIDEO, DOC, POLL, GROUP_CHAT_INVITE }

enum class CursorType {
    /** First message's cursor. */
    START,

    /** Last message's cursor. */
    END,
}

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It's
 * assumed that all enum values are lowercase in the DB.
 */
class PostgresEnum<T : Enum<T>>(postgresName: String, kotlinName: T?) : PGobject() {
    init {
        type = postgresName
        value = kotlinName?.name?.toLowerCase()
    }
}

/** Connects to the DB. This is safe to call multiple times. */
fun setUpDb() {
    db
}

/** Case-insensitively checks if [this] contains the [pattern]. */
infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). Private chats the
 * [userId] deleted are included.
 */
fun isUserInChat(userId: Int, chatId: Int): Boolean =
    chatId in PrivateChats.readIdList(userId) + GroupChatUsers.readChatIdList(userId)

fun readUserIdList(chatId: Int): Set<Int> =
    if (PrivateChats.isExisting(chatId)) PrivateChats.readUserIdList(chatId) else GroupChatUsers.readUserIdList(chatId)

/** Returns the ID of every user who the [userId] has a chat with (deleted private chats aren't included). */
fun readChatSharers(userId: Int): Set<Int> =
    PrivateChats.readOtherUserIdList(userId) + GroupChatUsers.readFellowParticipants(userId)

/**
 * Deletes the [userId]'s data from the DB, and [Notifier.unsubscribe]s them from all notifiers. An
 * [IllegalArgumentException] will be thrown if the not [GroupChatUsers.canUserLeave]. Nothing will happen if the
 * [userId] doesn't exist.
 *
 * ## Users
 *
 * - The [userId] will be deleted from the [Users].
 * - Users who have the [userId] in their contacts or chats will be notified of the [DeletedAccount].
 * - Clients who have [Notifier.subscribe]d via [groupChatsNotifier] will be [Notifier.unsubscribe]d.
 *
 * ## Blocked Users
 *
 * - Users who have blocked, or been blocked by the [userId] will be deleted from the blocked users list.
 *
 * ## Contacts
 *
 * - The user's [Contacts] will be deleted.
 * - Everyone's [Contacts] of the user will be deleted.
 * - Subscribers who have the [userId] in their contacts will be notified of this [DeletedContact] via
 *   [accountsNotifier].
 * - The [userId] will be unsubscribed via [accountsNotifier].
 *
 * ## Private Chats
 *
 * - Deletes every record the [userId] has in [PrivateChats] and [PrivateChatDeletions].
 * - Subscribers will be notified of a [DeletionOfEveryMessage] via [messagesNotifier].
 *
 * ## Group Chats
 *
 * - The [userId] will be removed from [GroupChats] they're in.
 * - If they're the last user in the group chat, the chat will be deleted from [GroupChats], [GroupChatUsers],
 *   [Messages], and [MessageStatuses].
 * - Clients will be [Notifier.unsubscribe]d via [groupChatsNotifier].
 *
 * ## Messages
 *
 * - Clients who have [Notifier.subscribe]d to [messagesNotifier]s will be notified of the [UserChatMessagesRemoval].
 * - Deletes all [Messages] and [MessageStatuses] the [userId] has sent.
 * - Clients will be [Notifier.unsubscribe]d via [messagesNotifier].
 *
 * ## Typing Statuses
 *
 * - Deletes [TypingStatuses] the [userId] created.
 * - The [userId] will be [Notifier.unsubscribe]d via [typingStatusesNotifier].
 */
fun deleteUser(userId: Int) {
    if (!GroupChatUsers.canUserLeave(userId))
        throw IllegalArgumentException(
            """
            The user's (ID: $userId) data can't be deleted because they're the last admin of a group chat with other 
            users.
            """,
        )
    accountsNotifier.publish(DeletedAccount(userId), Contacts.readOwners(userId) + readChatSharers(userId))
    Contacts.deleteUserEntries(userId)
    PrivateChats.deleteUserChats(userId)
    GroupChatUsers.removeUser(userId)
    TypingStatuses.deleteUser(userId)
    Messages.deleteUserMessages(userId)
    BlockedUsers.deleteUser(userId)
    Users.delete(userId)
    groupChatsNotifier.unsubscribe { it == userId }
    accountsNotifier.unsubscribe { it == userId }
    typingStatusesNotifier.unsubscribe { it == userId }
    messagesNotifier.unsubscribe { it == userId }
    onlineStatusesNotifier.unsubscribe { it == userId }
}
