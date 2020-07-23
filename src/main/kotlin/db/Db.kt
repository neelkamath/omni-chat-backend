package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject

data class ChatEdges(val chatId: Int, val edges: List<MessageEdge>)

data class ForwardPagination(val first: Int? = null, val after: Int? = null)

data class BackwardPagination(val last: Int? = null, val before: Int? = null)

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It's
 * assumed that all enum values are lowercase in the DB.
 */
class PostgresEnum<T : Enum<T>>(
    /** The name of the enum in Postgres. */
    typeName: String,
    /** The name of the enum in Kotlin. */
    value: T?
) : PGobject() {
    init {
        type = typeName
        this.value = value?.name?.toLowerCase()
    }
}

/**
 * Opens the DB connection, and creates the required types and tables. This must be run before any DB-related activities
 * are performed. This takes a small, but noticeable amount of time.
 */
fun setUpDb() {
    connect()
    create()
}

private fun connect() {
    val url = System.getenv("POSTGRES_URL")
    val db = System.getenv("POSTGRES_DB")
    Database.connect(
        "jdbc:postgresql://$url/$db?reWriteBatchedInserts=true",
        "org.postgresql.Driver",
        System.getenv("POSTGRES_USER"),
        System.getenv("POSTGRES_PASSWORD")
    )
}

/** Creates the required types and tables. */
private fun create(): Unit = transaction {
    createTypes()
    SchemaUtils.create(
        Pics,
        Contacts,
        Chats,
        GroupChats,
        GroupChatUsers,
        PrivateChats,
        PrivateChatDeletions,
        Stargazers,
        Messages,
        MessageStatuses,
        Users,
        TypingStatuses
    )
}

/** Whether [user1Id] and [user2Id] are in a chat with each other, including private chats deleted by only one user. */
fun shareChat(user1Id: Int, user2Id: Int): Boolean =
    PrivateChats.exists(user1Id, user2Id) || user1Id in GroupChatUsers.readFellowParticipants(user2Id)

/** Creates custom types if required. */
private fun createTypes(): Unit = transaction {
    mapOf("message_status" to MessageStatus.values(), "pic_type" to Pic.Type.values()).forEach { (name, enum) ->
        val values = enum.joinToString { "'${it.name.toLowerCase()}'" }
        if (!exists(name)) exec("CREATE TYPE $name AS ENUM ($values);")
    }
}

/** Whether the [type] has been created. */
private fun exists(type: String): Boolean = transaction {
    exec("SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = '$type');") { resultSet ->
        resultSet.next()
        val column = "exists"
        resultSet.getBoolean(column)
    }!!
}

/** Case-insensitively checks if [this] contains the [pattern]. */
infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). Private chats the
 * [userId] deleted are included.
 */
fun isUserInChat(userId: Int, chatId: Int): Boolean =
    chatId in PrivateChats.readIdList(userId) + GroupChatUsers.readChatIdList(userId)

/**
 * Deletes the [userId]'s data from the DB.
 *
 * ## Users
 *
 * - The [userId] will be deleted from the [Users].
 * - Clients who have [Broker.subscribe]d via [updatedChatsBroker] will be [Broker.unsubscribe]d.
 *
 * ## Contacts
 *
 * - The user's [Contacts] will be deleted.
 * - Everyone's [Contacts] of the user will be deleted.
 * - Clients who have [Broker.subscribe]d to [ContactsSubscription]s via the [contactsBroker], and have the
 *   [userId] in their [Contacts], will be notified of this [DeletedContact].
 * - The [userId] will be [Broker.unsubscribe]d from [ContactsSubscription]s if they've [Broker.subscribe]d via
 *   the [contactsBroker].
 *
 * ## Private Chats
 *
 * - Deletes every record the [userId] has in [PrivateChats] and [PrivateChatDeletions].
 * - Clients who have [Broker.subscribe]d via the [messagesBroker] will be notified of a [DeletionOfEveryMessage].
 *
 * ## Group Chats
 *
 * - The [userId] will be removed from [GroupChats] they're in.
 * - If they're the last user in the group chat, the chat will be deleted from [GroupChats], [GroupChatUsers],
 *   [Messages], and [MessageStatuses].
 * - Clients will be [Broker.unsubscribe]d via [updatedChatsBroker].
 *
 * ## Messages
 *
 * - Clients who have [Broker.subscribe]d to [MessagesAsset]s will be notified of the [UserChatMessagesRemoval].
 * - Deletes all [Messages] and [MessageStatuses] the [userId] has sent.
 * - Clients will be [Broker.unsubscribe]d via [messagesBroker].
 *
 * ## Typing Statuses
 *
 * - Deletes [TypingStatuses] the [userId] created.
 * - The [userId] will be [Broker.unsubscribe]d via [typingStatusesBroker].
 *
 * @throws [IllegalArgumentException] if the [userId] [GroupChats.isNonemptyChatAdmin].
 * @see [deleteUser]
 */
fun deleteUserFromDb(userId: Int) {
    if (GroupChats.isNonemptyChatAdmin(userId))
        throw IllegalArgumentException(
            "The user's (ID: $userId) data cannot be deleted because they're the admin of a nonempty group chat."
        )
    Contacts.deleteUserEntries(userId)
    PrivateChats.deleteUserChats(userId)
    GroupChatUsers.removeUser(userId)
    TypingStatuses.deleteUser(userId)
    Messages.deleteUserMessages(userId)
    Users.delete(userId)
    listOf(updatedChatsBroker, newGroupChatsBroker, contactsBroker, typingStatusesBroker, messagesBroker)
        .forEach { broker -> broker.unsubscribe { it.userId == userId } }
}