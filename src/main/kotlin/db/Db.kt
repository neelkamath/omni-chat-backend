package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.DeletionOfEveryMessage
import com.neelkamath.omniChat.UserChatMessagesRemoval
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It is
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
    createType("message_status", "ENUM ('delivery', 'read')")
    SchemaUtils.create(
        Contacts,
        GroupChats,
        GroupChatUsers,
        PrivateChats,
        PrivateChatDeletions,
        Messages,
        MessageStatuses
    )
}

/**
 * Creates the [definition] (e.g., `"ENUM ('delivery', 'read')"`) if the [name] (e.g., `"message_status"`) doesn't
 * exist.
 */
private fun createType(name: String, definition: String) {
    val exists = TransactionManager
        .current()
        .exec("SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = '$name');") { resultSet ->
            resultSet.next()
            val column = "exists"
            resultSet.getBoolean(column)
        }!!
    if (!exists) transact { exec("CREATE TYPE $name AS $definition;") }
}

/** Always use this instead of [transaction]. */
inline fun <T> transact(crossinline statement: Transaction.() -> T): T = transaction {
    addLogger(StdOutSqlLogger)
    statement()
}

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). If the [userId]
 * deleted the [chatId], and there was no activity after its deletion, it will appear as if the user isn't in the
 * [chatId].
 */
fun isUserInChat(userId: String, chatId: Int): Boolean =
    chatId in PrivateChats.readIdList(userId) || chatId in GroupChatUsers.readChatIdList(userId)

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
 * Deletes every chat the [userId] is in from [PrivateChats], [PrivateChatDeletions], [Messages], and
 * [MessageStatuses]. Clients will be notified of a [DeletionOfEveryMessage], and then [unsubscribeFromMessageUpdates].
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