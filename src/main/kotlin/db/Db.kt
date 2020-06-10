package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.DeletionOfEveryMessage
import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.UserChatMessagesRemoval
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject

/**
 * Pagination uses [Relay](https://relay.dev)'s
 * [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm) except that fields are only
 * nullable based on what's more logical.
 */
sealed class Pagination

/**
 * The [last] and [before] arguments indicate the number of items to be returned before the cursor.
 *
 * [last] indicates the maximum number of items to retrieve (e.g., if there are two items, and five gets requested, only
 * two will be returned). [before] is the cursor (i.e., only items before this will be returned). Here is the algorithm
 * for retrieving items:
 * - If neither [last] nor [before] are `null`, then at most [last] items will be returned from before the cursor.
 * - If [last] isn't null but [before] is, then at most [last] items will be returned from the end.
 * - If [last] is `null` but [before] isn't, then every item before the cursor will be returned.
 * - If both [last] and [before] are `null`, then every item will be returned.
 */
data class BackwardPagination(val last: Int? = null, val before: Int? = null) : Pagination()

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It's
 * assumed that all enum values are lowercase in the DB.
 */
class PostgresEnum<T : Enum<T>>(
    /** The name of the enum in Postgres. */
    typeName: String,
    /** The name of the enum in Kotlin */
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
private fun create(): Unit = transact {
    createTypes()
    SchemaUtils.create(
        Contacts,
        Chats,
        GroupChats,
        GroupChatUsers,
        PrivateChats,
        PrivateChatDeletions,
        Messages,
        MessageStatuses
    )
}

/** Creates custom types if required. */
private fun createTypes() {
    val values = MessageStatus.values().joinToString(", ") { "'${it.name.toLowerCase()}'" }
    createType("message_status", "ENUM ($values)")
}

/**
 * Creates the [name]'s (e.g., `"message_status"`) [definition] (e.g., `"ENUM ('delivered', 'read')"`) if it doesn't
 * exist.
 */
private fun createType(name: String, definition: String) {
    if (!exists(name)) transact { exec("CREATE TYPE $name AS $definition;") }
}

/** Whether the [type] has been created. */
private fun exists(type: String): Boolean =
    TransactionManager.current().exec("SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = '$type');") { resultSet ->
        resultSet.next()
        val column = "exists"
        resultSet.getBoolean(column)
    }!!

/** Always use this instead of [transaction]. */
inline fun <T> transact(crossinline statement: Transaction.() -> T): T = transaction {
    addLogger(StdOutSqlLogger)
    statement()
}

/**
 * Throws an [IllegalArgumentException] if the [userId] isn't in the chat [id] (chats include deleted private chats).
 *
 * @see [isUserInChat]
 */
fun readChat(id: Int, userId: String, pagination: BackwardPagination? = null): Chat = when (id) {
    in PrivateChats.readIdList(userId) -> PrivateChats.read(id, userId, pagination)
    in GroupChatUsers.readChatIdList(userId) -> GroupChats.read(id, pagination)
    else -> throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $id).")
}

/** Case-insensitively checks if [this] contains the [pattern]. */
infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). Private chats the
 * [userId] deleted are included.
 */
fun isUserInChat(userId: String, chatId: Int): Boolean =
    chatId in PrivateChats.readIdList(userId) + GroupChatUsers.readChatIdList(userId)

/**
 * Deletes the [userId]'s data. If the [userId] [GroupChats.isNonemptyChatAdmin], an [IllegalArgumentException] will be
 * thrown.
 *
 * ## Contacts
 *
 * The user's contacts will be deleted. Everyone's contacts of the user will be deleted.
 *
 * ## Private Chats
 *
 * Deletes every record the [userId] has in [PrivateChats], [PrivateChatDeletions], [Messages], and [MessageStatuses].
 * Clients will be notified of a [DeletionOfEveryMessage], and then [unsubscribeFromMessageUpdates].
 *
 * ## Group Chats
 *
 * The [userId] will be removed from chats they're in. If they're the last user in the chat, the chat will be deleted
 * from [GroupChats], [GroupChatUsers], [Messages], and [MessageStatuses]. Clients will be
 * [unsubscribeFromMessageUpdates].
 *
 * ## Messages
 *
 * Deletes all [Messages] and [MessageStatuses] the [userId] has sent. Clients who have [subscribeToMessageUpdates] will
 * be notified of the [UserChatMessagesRemoval].
 */
fun deleteUserFromDb(userId: String) {
    if (GroupChats.isNonemptyChatAdmin(userId))
        throw IllegalArgumentException(
            "The user's (ID: $userId) data cannot be deleted because they are the admin of a nonempty group chat."
        )
    Contacts.deleteUserEntries(userId)
    PrivateChats.delete(userId)
    GroupChatUsers.readChatIdList(userId).forEach { GroupChatUsers.removeUsers(it, setOf(userId)) }
    Messages.delete(userId)
}