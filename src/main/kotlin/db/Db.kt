package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.Pic.Companion.MAX_BYTES
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.deleteUser
import com.neelkamath.omniChat.graphql.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PGobject
import javax.annotation.Generated

data class ChatEdges(val chatId: Int, val edges: List<MessageEdge>)

data class ForwardPagination(val first: Int? = null, val after: Int? = null)

data class BackwardPagination(val last: Int? = null, val before: Int? = null)

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [Pic.MAX_BYTES]. */
data class Pic(
    /** At most [MAX_BYTES]. */
    val bytes: ByteArray,
    val type: Type
) {
    init {
        if (bytes.size > MAX_BYTES)
            throw IllegalArgumentException("The pic mustn't exceed $MAX_BYTES bytes.")
    }

    enum class Type {
        PNG {
            override fun toString() = "png"
        },
        JPEG {
            override fun toString() = "jpg"
        };

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"pjpeg"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension) {
                "png" -> PNG
                "jpg", "jpeg", "jfif", "pjpeg", "pjp" -> JPEG
                else ->
                    throw IllegalArgumentException("The pic ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pic

        if (!bytes.contentEquals(other.bytes)) return false
        if (type != other.type) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    companion object {
        const val MAX_BYTES = 25 * 1024 * 1024
    }
}

enum class MessageType { TEXT, PIC, AUDIO, VIDEO, POLL }

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It's
 * assumed that all enum values are lowercase in the DB.
 */
class PostgresEnum<T : Enum<T>>(postgresName: String, kotlinName: T?) : PGobject() {
    init {
        type = postgresName
        this.value = kotlinName?.name?.toLowerCase()
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
        TextMessages,
        PicMessages,
        AudioMessages,
        VideoMessages,
        PollMessages,
        PollOptions,
        PollVotes,
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
    mapOf(
        "message_status" to MessageStatus.values(),
        "pic_type" to Pic.Type.values(),
        "message_type" to MessageType.values()
    ).forEach { (name, enum) ->
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

fun readUserIdList(chatId: Int): List<Int> =
    if (PrivateChats.exists(chatId)) PrivateChats.readUserIdList(chatId) else GroupChatUsers.readUserIdList(chatId)

/** Returns the ID of every user who shares a chat with the [userId], including deleted private chats. */
fun readChatSharers(userId: Int): List<Int> =
    PrivateChats.readOtherUserIdList(userId) + GroupChatUsers.readFellowParticipants(userId)

/**
 * Deletes the [userId]'s data from the DB, and [Notifier.unsubscribe]s them from all notifiers. An
 * [IllegalArgumentException] will be thrown if the not [GroupChatUsers.canUserLeave].
 *
 * ## Users
 *
 * - The [userId] will be deleted from the [Users].
 * - Clients who have [Notifier.subscribe]d via [updatedChatsNotifier] will be [Notifier.unsubscribe]d.
 *
 * ## Contacts
 *
 * - The user's [Contacts] will be deleted.
 * - Everyone's [Contacts] of the user will be deleted.
 * - Subscribers who have the [userId] in their contacts will be notified of this [DeletedContact] via [contactsNotifier].
 * - The [userId] will be unsubscribed via [contactsNotifier].
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
 * - Clients will be [Notifier.unsubscribe]d via [updatedChatsNotifier].
 *
 * ## Messages
 *
 * - Clients who have [Notifier.subscribe]d to [MessagesAsset]s will be notified of the [UserChatMessagesRemoval].
 * - Deletes all [Messages] and [MessageStatuses] the [userId] has sent.
 * - Clients will be [Notifier.unsubscribe]d via [messagesNotifier].
 *
 * ## Typing Statuses
 *
 * - Deletes [TypingStatuses] the [userId] created.
 * - The [userId] will be [Notifier.unsubscribe]d via [typingStatusesNotifier].
 *
 * @see [deleteUser]
 */
fun deleteUserFromDb(userId: Int) {
    if (!GroupChatUsers.canUserLeave(userId))
        throw IllegalArgumentException(
            """
            The user's (ID: $userId) data can't be deleted because they're the last admin of a group chat with other 
            users.
            """
        )
    Contacts.deleteUserEntries(userId)
    PrivateChats.deleteUserChats(userId)
    GroupChatUsers.removeUser(userId)
    TypingStatuses.deleteUser(userId)
    Messages.deleteUserMessages(userId)
    Users.delete(userId)
    updatedChatsNotifier.unsubscribe { it.userId == userId }
    newGroupChatsNotifier.unsubscribe { it.userId == userId }
    contactsNotifier.unsubscribe { it.userId == userId }
    typingStatusesNotifier.unsubscribe { it.userId == userId }
    messagesNotifier.unsubscribe { it.userId == userId }
    onlineStatusesNotifier.unsubscribe { it.userId == userId }
}