package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import org.keycloak.representations.idm.UserRepresentation
import java.time.LocalDateTime

typealias Cursor = Int

/**
 * @throws [IllegalArgumentException] if the [value] isn't lowercase, isn't shorter than 256 characters, or doesn't
 * contain non-whitespace characters.
 */
data class Username(val value: String) {
    init {
        if (value.trim().isEmpty()) throw IllegalArgumentException("""The username ("$value") mustn't be empty.""")
        // The auth system disallows usernames longer than 255 characters.
        if (value.length > 255) throw IllegalArgumentException("The username($value) must be less than 256 characters.")
        // The auth system silently saves uppercase characters in usernames as lowercase.
        if (value != value.toLowerCase()) throw IllegalArgumentException("The username ($value) must be lowercase.")
    }
}

/** @throws [IllegalArgumentException] if the [value] doesn't contain non-whitespace characters. */
data class Password(val value: String) {
    init {
        if (value.trim().isEmpty()) throw IllegalArgumentException("""The password ("$value") mustn't be empty.""")
    }
}

object Placeholder

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any?>? = null,
    val operationName: String? = null
)

data class GraphQlResponse(val data: Map<String, Any?>? = null, val errors: List<GraphQlResponseError>? = null)

data class GraphQlResponseError(val message: String)

data class Login(val username: Username, val password: Password)

data class TokenSet(val accessToken: String, val refreshToken: String)

data class NewAccount(
    val username: Username,
    val password: Password,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null
)

interface AccountData {
    val id: String
    val username: Username
    val emailAddress: String
    val firstName: String?
    val lastName: String?
    val bio: String?
}

data class Account(
    override val id: String,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: String? = null
) : AccountData {
    /**
     * Case-insensitively searches for the [query] in the [UserRepresentation.username], [UserRepresentation.firstName],
     * [UserRepresentation.lastName], and [UserRepresentation.email].
     */
    fun matches(query: String): Boolean =
        listOfNotNull(username.value, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }
}

interface ContactsSubscription

data class NewContact(
    override val id: String,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: String? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: String): NewContact =
            with(readUserById(userId)) { NewContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

data class UpdatedContact(
    override val id: String,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: String? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: String): UpdatedContact =
            with(readUserById(userId)) { UpdatedContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

data class DeletedContact(val id: String) : ContactsSubscription

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class NewGroupChat(
    val title: GroupChatTitle,
    val description: GroupChatDescription,
    val userIdList: List<String> = listOf()
)

private fun <T> verifyGroupChatUsers(newUsers: List<T>?, removedUsers: List<T>?) {
    if (newUsers != null && removedUsers != null) {
        val intersection = newUsers.intersect(removedUsers)
        if (intersection.isNotEmpty())
            throw IllegalArgumentException(
                "The list of new and removed users must be distinct. Users in both lists: $intersection"
            )
    }
}

interface UpdatedChatsSubscription

/**
 * @throws [IllegalArgumentException] if the [value] isn't 1-[Messages.MAX_TEXT_LENGTH] characters with at least one
 * non-whitespace.
 */
data class TextMessage(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > Messages.MAX_TEXT_LENGTH)
            throw IllegalArgumentException(
                "The text must be 1-${Messages.MAX_TEXT_LENGTH} characters, with at least one non-whitespace."
            )
    }
}

/**
 * @throws [IllegalArgumentException] if the [value] isn't 1-[GroupChats.MAX_TITLE_LENGTH] characters, of which at
 * least one isn't whitespace.
 */
data class GroupChatTitle(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > GroupChats.MAX_TITLE_LENGTH)
            throw IllegalArgumentException(
                """
                The title ("$value") must be 1-${GroupChats.MAX_TITLE_LENGTH} characters, with at least one 
                non-whitespace character.
                """.trimIndent()
            )
    }
}

/** @throws [IllegalArgumentException] if the [value] isn't at most [GroupChats.MAX_DESCRIPTION_LENGTH] characters. */
data class GroupChatDescription(val value: String) {
    init {
        if (value.length > GroupChats.MAX_DESCRIPTION_LENGTH)
            throw IllegalArgumentException(
                """The description ("$value") must be at most ${GroupChats.MAX_DESCRIPTION_LENGTH} characters"""
            )
    }
}

/** @throws [IllegalArgumentException] if the [newUsers] and [removedUsers] aren't distinct. */
data class UpdatedGroupChat(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUsers: List<Account>? = null,
    val removedUsers: List<Account>? = null,
    val adminId: String? = null
) : UpdatedChatsSubscription {
    init {
        verifyGroupChatUsers(newUsers, removedUsers)
    }
}

data class UpdatedAccount(
    val userId: String,
    val username: Username,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null
) : UpdatedChatsSubscription {
    companion object {
        fun build(userId: String): UpdatedAccount =
            with(readUserById(userId)) { UpdatedAccount(userId, username, emailAddress, firstName, lastName) }
    }
}

/** @throws [IllegalArgumentException] if the [newUserIdList] and [removedUserIdList] aren't distinct. */
data class GroupChatUpdate(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUserIdList: List<String>? = listOf(),
    val removedUserIdList: List<String>? = listOf(),
    val newAdminId: String? = null
) {
    init {
        verifyGroupChatUsers(newUserIdList, removedUserIdList)
    }

    fun toUpdatedGroupChat(): UpdatedGroupChat = UpdatedGroupChat(
        chatId,
        title,
        description,
        newUserIdList?.map(::readUserById),
        removedUserIdList?.map(::readUserById),
        newAdminId
    )
}

interface Chat {
    val id: Int
    val messages: MessagesConnection
}

data class PrivateChat(
    override val id: Int,
    val user: Account,
    override val messages: MessagesConnection
) : Chat

data class GroupChat(
    override val id: Int,
    val adminId: String,
    val users: AccountsConnection,
    val title: GroupChatTitle,
    val description: GroupChatDescription? = null,
    override val messages: MessagesConnection
) : Chat

data class MessagesConnection(val edges: List<MessageEdge>, val pageInfo: PageInfo)

data class MessageEdge(val node: Message, val cursor: Cursor)

interface MessageData {
    val chatId: Int
    val messageId: Int
    val sender: Account
    val text: TextMessage
    val dateTimes: MessageDateTimes
}

data class Message(
    val id: Int,
    val sender: Account,
    val text: TextMessage,
    val dateTimes: MessageDateTimes
) {
    fun toNewMessage(): NewMessage = NewMessage(Messages.readChatFromMessage(id), id, sender, text, dateTimes)

    fun toUpdatedMessage(): UpdatedMessage =
        UpdatedMessage(Messages.readChatFromMessage(id), id, sender, text, dateTimes)
}

interface MessagesSubscription

data class NewMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes
) : MessageData, MessagesSubscription

data class UpdatedMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes
) : MessageData, MessagesSubscription

data class MessageDateTimes(val sent: LocalDateTime, val statuses: List<MessageDateTimeStatus> = listOf())

data class MessageDateTimeStatus(val user: Account, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val chatId: Int, val messageId: Int) : MessagesSubscription

data class MessageDeletionPoint(val chatId: Int, val until: LocalDateTime) : MessagesSubscription

data class UserChatMessagesRemoval(val chatId: Int, val userId: String) : MessagesSubscription

data class ExitedUser(val chatId: Int, val userId: String) : UpdatedChatsSubscription

interface NewGroupChatsSubscription

data class GroupChatId(val id: Int) : NewGroupChatsSubscription

data class DeletionOfEveryMessage(val chatId: Int) : MessagesSubscription

object CreatedSubscription :
    MessagesSubscription,
    ContactsSubscription,
    UpdatedChatsSubscription,
    NewGroupChatsSubscription {

    val placeholder = Placeholder
}

data class ChatMessages(val chat: Chat, val messages: List<MessageEdge>)

data class AccountsConnection(val edges: List<AccountEdge>, val pageInfo: PageInfo) {
    companion object {
        /** @param[AccountEdges] needn't be listed in ascending order of their [AccountEdge.cursor]. */
        fun build(AccountEdges: List<AccountEdge>, pagination: ForwardPagination? = null): AccountsConnection {
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
    }
}

data class AccountEdge(val node: Account, val cursor: Cursor)

data class PageInfo(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: Cursor? = null,
    val endCursor: Cursor? = null
)