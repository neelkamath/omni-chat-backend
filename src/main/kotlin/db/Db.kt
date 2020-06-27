package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.chats.*
import com.neelkamath.omniChat.db.contacts.Contacts
import com.neelkamath.omniChat.db.contacts.subscribeToContactUpdates
import com.neelkamath.omniChat.db.messages.MessageStatuses
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.db.messages.subscribeToMessageUpdates
import com.neelkamath.omniChat.db.messages.unsubscribeUserFromMessageUpdates
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
        MessageStatuses,
        Users
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

/** Case-insensitively checks if [this] contains the [pattern]. */
infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). Private chats the
 * [userId] deleted are included.
 */
fun isUserInChat(userId: String, chatId: Int): Boolean =
    chatId in PrivateChats.readIdList(userId) + GroupChatUsers.readChatIdList(userId)

/** @param[AccountEdges] needn't be listed in ascending order of their [AccountEdge.cursor]. */
fun buildAccountsConnection(
    AccountEdges: List<AccountEdge>,
    pagination: ForwardPagination? = null
): AccountsConnection {
    val (first, after) = pagination ?: ForwardPagination()
    val accounts = AccountEdges.sortedBy { it.cursor }
    val afterAccounts = if (after == null) accounts else accounts.filter { it.cursor > after }
    val firstAccounts = if (first == null) afterAccounts else afterAccounts.take(first)
    val edges = firstAccounts.map { AccountEdge(it.node, cursor = it.cursor) }
    val pageInfo = PageInfo(
        hasNextPage = firstAccounts.size < afterAccounts.size,
        hasPreviousPage = afterAccounts.size < accounts.size,
        startCursor = accounts.firstOrNull()?.cursor,
        endCursor = accounts.lastOrNull()?.cursor
    )
    return AccountsConnection(edges, pageInfo)
}

/**
 * Deletes the [userId]'s data. If the [userId] [GroupChats.isNonemptyChatAdmin], an [IllegalArgumentException] will be
 * thrown.
 *
 * ## Users
 *
 * The [userId] will be deleted from the [Users].
 *
 * ## Contacts
 *
 * The user's contacts will be deleted. Everyone's contacts of the user will be deleted. Clients who have
 * [subscribeToContactUpdates], and have the [userId] in their contacts, will be notified of the [DeletedContact].
 *
 * ## Private Chats
 *
 * Deletes every record the [userId] has in [PrivateChats], [PrivateChatDeletions], [Messages], and [MessageStatuses].
 * Clients will be notified of a [DeletionOfEveryMessage], and then [unsubscribeUserFromMessageUpdates].
 *
 * ## Group Chats
 *
 * The [userId] will be removed from chats they're in. If they're the last user in the chat, the chat will be deleted
 * from [GroupChats], [GroupChatUsers], [Messages], and [MessageStatuses]. Clients will be
 * [unsubscribeUserFromMessageUpdates].
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
    Users.delete(userId)
    Contacts.deleteUserEntries(userId)
    PrivateChats.deleteUserChats(userId)
    GroupChatUsers.readChatIdList(userId).forEach { GroupChatUsers.removeUsers(it, listOf(userId)) }
    Messages.deleteUserMessages(userId)
}