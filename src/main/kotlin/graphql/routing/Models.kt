package com.neelkamath.omniChatBackend.graphql.routing

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.MessageType
import com.neelkamath.omniChatBackend.db.tables.*
import java.time.LocalDateTime
import java.util.*

typealias Cursor = Int

/**
 * An [IllegalArgumentException] will be thrown if the following criteria aren't met:
 * - Must be 1-30 characters.
 * - Must contain only lowercase English letters (a-z), English numbers (0-9), periods, and underscores.
 */
data class Username(val value: String) {
    init {
        require(!value.contains(Regex("""[^a-z0-9._]"""))) {
            """
            The username ("$value") must only contain lowercase English letters, English numbers, periods, and 
            underscores.
            """
        }
        require(value.length in 1..30) { "The username ($value) must be 1-${Users.MAX_NAME_LENGTH} characters long." }
    }
}

data class DeletedAccount(val id: Int) : AccountsSubscription

/**
 * An [IllegalArgumentException] will be thrown if it contains whitespace, or exceeds [Users.MAX_NAME_LENGTH]
 * characters.
 */
data class Name(val value: String) {
    init {
        require(!value.contains(Regex("""\s"""))) { """The name ("$value") cannot contain whitespace.""" }
        require(value.length <= Users.MAX_NAME_LENGTH) {
            "The name ($value) mustn't exceed ${Users.MAX_NAME_LENGTH} characters."
        }
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] exceeds [Bio.MAX_LENGTH], contains leading whitespace, or
 * contains trailing whitespace.
 */
data class Bio(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { "The value ($value) cannot exceed $MAX_LENGTH characters." }
        require(value.trim() == value) { "The value ($value) can neither contain leading nor trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 2500
    }
}

/** An [IllegalArgumentException] will be thrown if the [value] doesn't contain non-whitespace characters. */
data class Password(val value: String) {
    init {
        require(value.trim().isNotEmpty()) { """The password ("$value") mustn't be empty.""" }
    }
}

object Placeholder

object NonexistentUser : RequestTokenSetResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object CannotLeaveChat : LeaveGroupChatResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object UnverifiedEmailAddress : RequestTokenSetResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

data class CreatedChatId(val id: Int) : CreateGroupChatResult, CreatePrivateChatResult

object EmailAddressVerified : EmailEmailAddressVerificationResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object UsernameTaken : UpdateAccountResult, CreateAccountResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object IncorrectPassword : RequestTokenSetResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object EmailAddressTaken : UpdateAccountResult, CreateAccountResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidChatId :
    SearchChatMessagesResult,
    ReadChatResult,
    CreateTextMessageResult,
    CreateActionMessageResult,
    CreateGroupChatInviteMessageResult,
    CreatePollMessageResult,
    ForwardMessageResult,
    LeaveGroupChatResult {

    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidAdminId : CreateGroupChatResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidVerificationCode : VerifyEmailAddressResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object UnregisteredEmailAddress : VerifyEmailAddressResult, ResetPasswordResult, EmailEmailAddressVerificationResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidPasswordResetCode : ResetPasswordResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidUserId : CreatePrivateChatResult, ReadOnlineStatusResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidMessageId :
    CreateTextMessageResult,
    CreateActionMessageResult,
    CreateGroupChatInviteMessageResult,
    CreatePollMessageResult,
    ForwardMessageResult,
    TriggerActionResult,
    SetPollVoteResult {

    @Suppress("unused")
    val placeholder = Placeholder
}

object CannotDeleteAccount {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidPoll : CreatePollMessageResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object NonexistentOption : SetPollVoteResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidInviteCode : ReadGroupChatResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidInvitedChat : CreateGroupChatInviteMessageResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidDomain : CreateAccountResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

object InvalidAction : CreateActionMessageResult, TriggerActionResult {
    @Suppress("unused")
    val placeholder = Placeholder
}

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any?>? = null,
    val operationName: String? = null,
)

data class GraphQlResponse(val data: Map<String, Any?>? = null, val errors: List<GraphQlResponseError>? = null)

data class GraphQlResponseError(val message: String)

data class Login(val username: Username, val password: Password)

data class TokenSet(val accessToken: String, val refreshToken: String) : RequestTokenSetResult

data class AccountInput(
    val username: Username,
    val password: Password,
    val emailAddress: String,
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val bio: Bio = Bio(""),
)

interface AccountData {
    val id: Int
    val username: Username
    val emailAddress: String
    val firstName: Name
    val lastName: Name
    val bio: Bio
}

data class Account(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio
) : AccountData {
    /**
     * Case-insensitively [query]s the [username], [firstName], [lastName], and [emailAddress].
     */
    fun matches(query: String): Boolean = listOfNotNull(username.value, firstName.value, lastName.value, emailAddress)
        .any { it.contains(query, ignoreCase = true) }
}

data class MessageContext(val hasContext: Boolean, val id: Int?)

enum class GroupChatPublicity { NOT_INVITABLE, INVITABLE, PUBLIC }

interface AccountsSubscription

data class NewContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio,
) : AccountData, AccountsSubscription {
    companion object {
        fun build(userId: Int): NewContact =
            with(Users.read(userId)) { NewContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

data class StarredMessagesConnection(val edges: List<StarredMessageEdge>, val pageInfo: PageInfo)

data class StarredMessageEdge(val node: StarredMessage, val cursor: Cursor)

interface OnlineStatusesSubscription

data class TypingUsers(val chatId: Int, val users: List<Account>)

data class OnlineStatus(val userId: Int, val isOnline: Boolean, val lastOnline: LocalDateTime?) :
    ReadOnlineStatusResult,
    OnlineStatusesSubscription

data class DeletedContact(val id: Int) : AccountsSubscription

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: Name? = null,
    val lastName: Name? = null,
    val bio: Bio? = null,
)

/**
 * An [IllegalArgumentException] will be thrown if the [adminIdList] is empty, or the [adminIdList] isn't a subset of
 * the [userIdList].
 */
data class GroupChatInput(
    val title: GroupChatTitle,
    val description: GroupChatDescription,
    val userIdList: List<Int>,
    val adminIdList: List<Int>,
    val isBroadcast: Boolean,
    val publicity: GroupChatPublicity,
) {
    init {
        require(adminIdList.isNotEmpty()) { "There must be at least one admin." }
        require(userIdList.containsAll(adminIdList)) {
            "The admin ID list ($adminIdList) must be a subset of the user ID list ($userIdList)."
        }
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[MessageText.MAX_LENGTH] characters with at least
 * one non-whitespace character, or it contains leading/trailing whitespace.
 */
data class MessageText(val value: String) {
    init {
        require(value.trim().isNotEmpty() && value.length <= MAX_LENGTH) {
            "The text must be 1-$MAX_LENGTH characters, with at least one non-whitespace."
        }
        require(value.trim() == value) { "The value ($value) mustn't contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 10_000
    }
}

/**
 * An [IllegalArgumentException] will be thrown in the following cases:
 * - The [value] isn't 1-[GroupChatTitle.MAX_LENGTH] characters, of which at least one isn't whitespace.
 * - The [value] contains leading or trailing whitespace.
 */
data class GroupChatTitle(val value: String) {
    init {
        require(value.trim().isNotEmpty() && value.length <= MAX_LENGTH) {
            """The title ("$value") must be 1-$MAX_LENGTH characters, with at least one non-whitespace character."""
        }
        require(value.trim() == value) { "The value ($value) cannot contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 70
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't at most [GroupChatDescription.MAX_LENGTH]
 * characters, or it contains leading/trailing whitespace.
 */
data class GroupChatDescription(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { """The description ("$value") must be at most $MAX_LENGTH characters""" }
        require(value.trim() == value) { "The value ($value) mustn't contain leading or trailing whitespace." }
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

/** A blocked user. */
data class BlockedAccount(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio,
) : AccountsSubscription, AccountData {
    companion object {
        fun build(userId: Int): BlockedAccount =
            with(Users.read(userId)) { BlockedAccount(id, username, emailAddress, firstName, lastName, bio) }
    }
}

/** An unblocked user. */
data class UnblockedAccount(val id: Int) : AccountsSubscription

/** An [IllegalArgumentException] will be thrown if the [newUsers] and [removedUsers] aren't distinct. */
data class UpdatedGroupChat(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUsers: List<Account>? = null,
    val removedUsers: List<Account>? = null,
    val adminIdList: List<Int>? = null,
    val isBroadcast: Boolean? = null,
    val publicity: GroupChatPublicity? = null,
) : GroupChatsSubscription {
    init {
        if (newUsers != null && removedUsers != null) {
            val intersection = newUsers.intersect(removedUsers)
            require(intersection.isEmpty()) {
                "The list of new and removed users must be distinct. Users in both lists: $intersection."
            }
        }
    }
}

interface TypingStatusesSubscription

data class TypingStatus(val chatId: Int, val userId: Int, val isTyping: Boolean) : TypingStatusesSubscription

data class UpdatedProfilePic(val id: Int) : AccountsSubscription

data class UpdatedGroupChatPic(val id: Int) : GroupChatsSubscription

data class UpdatedAccount(
    val id: Int,
    val username: Username,
    val emailAddress: String,
    val firstName: Name,
    val lastName: Name,
    val bio: Bio,
) : AccountsSubscription {
    companion object {
        fun build(userId: Int): UpdatedAccount =
            with(Users.read(userId)) { UpdatedAccount(userId, username, emailAddress, firstName, lastName, bio) }
    }
}

interface Chat {
    val id: Int
    val messages: MessagesConnection
}

data class PrivateChat(
    override val id: Int,
    val user: Account,
    override val messages: MessagesConnection,
) : Chat, ReadChatResult

interface BareGroupChat {
    val title: GroupChatTitle
    val description: GroupChatDescription
    val adminIdList: List<Int>
    val users: AccountsConnection
    val isBroadcast: Boolean
    val publicity: GroupChatPublicity
}

data class GroupChatInfo(
    override val adminIdList: List<Int>,
    override val users: AccountsConnection,
    override val title: GroupChatTitle,
    override val description: GroupChatDescription,
    override val isBroadcast: Boolean,
    override val publicity: GroupChatPublicity,
) : BareGroupChat, ReadGroupChatResult

data class GroupChat(
    override val id: Int,
    override val adminIdList: List<Int>,
    override val users: AccountsConnection,
    override val title: GroupChatTitle,
    override val description: GroupChatDescription,
    override val messages: MessagesConnection,
    override val isBroadcast: Boolean,
    override val publicity: GroupChatPublicity,
    val inviteCode: UUID?
) : Chat, BareGroupChat, ReadChatResult

data class GroupChatsConnection(val edges: List<GroupChatEdge>, val pageInfo: PageInfo)

data class GroupChatEdge(val node: GroupChat, val cursor: Cursor)

data class MessagesConnection(val edges: List<MessageEdge>, val pageInfo: PageInfo)

data class MessageEdge(val node: Message, val cursor: Cursor)

data class UnstarredChat(val id: Int) : MessagesSubscription

interface Message {
    val messageId: Int
    val sender: Account
    val state: MessageState
    val sent: LocalDateTime
    val statuses: List<MessageDateTimeStatus>
    val context: MessageContext
    val isForwarded: Boolean
    val hasStar: Boolean

    fun toUpdatedMessage(): UpdatedMessage =
        UpdatedMessage(Messages.readChatIdFromMessageId(messageId), messageId, state, statuses, hasStar)

    fun toStarredTextMessage(): StarredTextMessage = StarredTextMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
        TextMessages.read(messageId),
    )

    fun toStarredActionMessage(): StarredActionMessage = StarredActionMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
        ActionMessages.read(messageId),
    )

    fun toStarredPicMessage(): StarredPicMessage = StarredPicMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
        PicMessages.read(messageId).caption,
    )

    fun toStarredAudioMessage(): StarredAudioMessage = StarredAudioMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
    )

    fun toStarredGroupChatInviteMessage(): StarredGroupChatInviteMessage {
        val chatId = Messages.readChatIdFromMessageId(messageId)
        return StarredGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            state,
            sent,
            statuses,
            context,
            isForwarded,
            GroupChats.readInviteCode(chatId),
        )
    }

    fun toStarredDocMessage(): StarredDocMessage = StarredDocMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
    )

    fun toStarredVideoMessage(): StarredVideoMessage = StarredVideoMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
    )

    fun toStarredPollMessage(): StarredPollMessage = StarredPollMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        state,
        sent,
        statuses,
        context,
        isForwarded,
        PollMessages.read(messageId),
    )
}

data class TextMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val textMessage: MessageText,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): TextMessage = with(message) {
            TextMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                TextMessages.read(messageId),
            )
        }
    }
}

data class ActionMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val actionableMessage: ActionableMessage,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): ActionMessage = with(message) {
            ActionMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                ActionMessages.read(messageId),
            )
        }
    }
}

data class PicMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val caption: MessageText?,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): PicMessage = with(message) {
            PicMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PicMessages.read(messageId).caption,
            )
        }
    }
}

data class AudioMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): AudioMessage = with(message) {
            AudioMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
            )
        }
    }
}

data class GroupChatInviteMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val inviteCode: UUID,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): GroupChatInviteMessage = with(message) {
            GroupChatInviteMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                GroupChats.readInviteCode(Messages.readChatIdFromMessageId(messageId)),
            )
        }
    }
}

data class DocMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): DocMessage = with(message) {
            DocMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
            )
        }
    }
}

data class VideoMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): VideoMessage = with(message) {
            VideoMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
            )
        }
    }
}

data class PollMessage(
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val poll: Poll,
) : Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: Messages.RawMessage, userId: Int? = null): PollMessage = with(message) {
            PollMessage(
                messageId,
                sender,
                state,
                sent,
                statuses,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PollMessages.read(messageId),
            )
        }
    }
}

interface StarredMessage {
    val chatId: Int
    val messageId: Int
    val sender: Account
    val state: MessageState
    val sent: LocalDateTime
    val statuses: List<MessageDateTimeStatus>
    val context: MessageContext
    val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId] as seen by the [userId]. */
        fun build(userId: Int, messageId: Int): StarredMessage =
            when (val message = Messages.readMessage(userId, messageId)) {
                is TextMessage -> message.toStarredTextMessage()
                is ActionMessage -> message.toStarredActionMessage()
                is PicMessage -> message.toStarredPicMessage()
                is AudioMessage -> message.toStarredAudioMessage()
                is GroupChatInviteMessage -> message.toStarredGroupChatInviteMessage()
                is DocMessage -> message.toStarredDocMessage()
                is VideoMessage -> message.toStarredVideoMessage()
                is PollMessage -> message.toStarredPollMessage()
                else -> throw IllegalArgumentException("$message didn't match a concrete type.")
            }
    }
}

data class StarredTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val textMessage: MessageText,
) : StarredMessage

data class StarredActionMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val actionableMessage: ActionableMessage,
) : StarredMessage

data class StarredPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?,
) : StarredMessage

data class StarredPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll,
) : StarredMessage

data class StarredAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : StarredMessage

data class StarredGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID,
) : StarredMessage

data class StarredDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : StarredMessage

data class StarredVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val statuses: List<MessageDateTimeStatus>,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : StarredMessage

interface MessagesSubscription

interface NewMessage {
    val chatId: Int
    val messageId: Int
    val sender: Account
    val state: MessageState
    val sent: LocalDateTime
    val context: MessageContext
    val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId]. */
        fun build(messageId: Int): NewMessage {
            val message = Messages.readRawMessage(messageId)
            return when (message.type) {
                MessageType.TEXT -> message.toNewTextMessage()
                MessageType.ACTION -> message.toNewActionMessage()
                MessageType.PIC -> message.toNewPicMessage()
                MessageType.AUDIO -> message.toNewAudioMessage()
                MessageType.GROUP_CHAT_INVITE -> message.toNewGroupChatInviteMessage()
                MessageType.DOC -> message.toNewDocMessage()
                MessageType.VIDEO -> message.toNewVideoMessage()
                MessageType.POLL -> message.toNewPollMessage()
            }
        }
    }
}

data class NewTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val textMessage: MessageText,
) : NewMessage, MessagesSubscription

data class NewActionMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val actionableMessage: ActionableMessage,
) : NewMessage, MessagesSubscription

data class NewPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?,
) : NewMessage, MessagesSubscription

data class NewPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll,
) : NewMessage, MessagesSubscription

data class NewAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : NewMessage, MessagesSubscription

data class NewGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID,
) : NewMessage, MessagesSubscription

data class NewDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : NewMessage, MessagesSubscription

data class NewVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val state: MessageState,
    override val sent: LocalDateTime,
    override val context: MessageContext,
    override val isForwarded: Boolean,
) : NewMessage, MessagesSubscription

data class UpdatedMessage(
    val chatId: Int,
    val messageId: Int,
    val state: MessageState,
    val statuses: List<MessageDateTimeStatus>,
    val hasStar: Boolean,
) : MessagesSubscription

enum class MessageState { SENT, DELIVERED, READ }

data class MessageDateTimeStatus(val user: Account, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val chatId: Int, val messageId: Int) : MessagesSubscription

data class MessageDeletionPoint(val chatId: Int, val until: LocalDateTime) : MessagesSubscription

data class UserChatMessagesRemoval(val chatId: Int, val userId: Int) : MessagesSubscription

data class ExitedUsers(val chatId: Int, val userIdList: List<Int>) : GroupChatsSubscription

interface GroupChatsSubscription

interface ReadOnlineStatusResult

data class GroupChatId(val id: Int) : GroupChatsSubscription

data class DeletionOfEveryMessage(val chatId: Int) : MessagesSubscription

object CreatedSubscription :
    MessagesSubscription,
    AccountsSubscription,
    GroupChatsSubscription,
    TypingStatusesSubscription,
    OnlineStatusesSubscription {

    @Suppress("unused")
    val placeholder = Placeholder
}

data class ChatMessagesConnection(val edges: List<ChatMessagesEdge>, val pageInfo: PageInfo)

data class ChatMessagesEdge(val node: ChatMessages, val cursor: Cursor)

data class ChatsConnection(val edges: List<ChatEdge>, val pageInfo: PageInfo)

data class ChatEdge(val node: Chat, val cursor: Cursor)

data class ChatMessages(val chat: Chat, val messages: List<MessageEdge>)

data class AccountsConnection(val edges: List<AccountEdge>, val pageInfo: PageInfo) {
    companion object {
        /**
         * An [AccountsConnection] is used when the dataset used is too large to load in one go. Certain datasets are
         * considered large in the context of networking but not in a server program. For example, a list of thousands
         * of elements has a negligible performance impact on a server but is too large to transfer as JSON to a client.
         * In such cases, you can easily build an [AccountsConnection] by passing the entire dataset to this function.
         * Of course, if the dataset would be considered large by a server program as well, then this function shouldn't
         * be used since the [accountEdges] passed must contain the entire dataset.
         *
         * The [accountEdges] will be sorted in ascending order of their [AccountEdge.cursor] for you.
         */
        fun build(accountEdges: Set<AccountEdge>, pagination: ForwardPagination? = null): AccountsConnection {
            val (first, after) = pagination ?: ForwardPagination()
            val accounts = accountEdges.sortedBy { it.cursor }
            val afterAccounts = if (after == null) accounts else accounts.filter { it.cursor > after }
            val firstAccounts = if (first == null) afterAccounts else afterAccounts.take(first)
            val edges = firstAccounts.map { AccountEdge(it.node, it.cursor) }
            val pageInfo = PageInfo(
                hasNextPage = firstAccounts.size < afterAccounts.size,
                hasPreviousPage = afterAccounts.size < accounts.size,
                startCursor = accounts.firstOrNull()?.cursor,
                endCursor = accounts.lastOrNull()?.cursor,
            )
            return AccountsConnection(edges, pageInfo)
        }
    }
}

data class AccountEdge(val node: Account, val cursor: Cursor) {
    companion object {
        fun build(userId: Int, cursor: Cursor): AccountEdge = AccountEdge(Users.read(userId).toAccount(), cursor)
    }
}

data class PageInfo(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: Cursor? = null,
    val endCursor: Cursor? = null,
) {
    companion object {
        fun build(
            lastEdgeCursor: Cursor?,
            startCursor: Cursor?,
            endCursor: Cursor?,
            pagination: ForwardPagination? = null,
        ): PageInfo {
            val hasNextPage = when {
                endCursor == null -> false
                lastEdgeCursor != null -> lastEdgeCursor < endCursor
                pagination?.after == null -> true
                else -> pagination.after < endCursor
            }
            val hasPreviousPage = when {
                startCursor == null -> false
                lastEdgeCursor != null -> startCursor < lastEdgeCursor
                pagination?.after == null -> false
                else -> startCursor <= pagination.after
            }
            return PageInfo(hasNextPage, hasPreviousPage, startCursor, endCursor)
        }
    }
}

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
private fun <T> validateOptions(options: Collection<T>) {
    require(options.size > 1) { "There must be at least two options: $options." }
    require(options.toSet().size == options.size) { "Options must be unique: $options." }
}

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class PollInput(val title: MessageText, val options: List<MessageText>) {
    init {
        validateOptions(options)
    }
}

data class PollOption(val option: MessageText, val votes: List<Int>)

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class Poll(val title: MessageText, val options: List<PollOption>) {
    init {
        validateOptions(options)
    }
}

/** An [IllegalArgumentException] is thrown if there isn't at least one [actions], or the [actions] aren't unique. */
data class ActionableMessage(val text: MessageText, val actions: List<MessageText>) {
    init {
        validateOptions(actions)
    }

    fun toActionMessageInput(): ActionMessageInput = ActionMessageInput(text, actions)
}

/** An [IllegalArgumentException] is thrown if there isn't at least one [actions], or the [actions] aren't unique. */
data class ActionMessageInput(val text: MessageText, val actions: List<MessageText>) {
    init {
        validateOptions(actions)
    }
}

data class TriggeredAction(val messageId: Int, val action: MessageText, val triggeredBy: Account) : MessagesSubscription

interface SearchChatMessagesResult

interface ReadChatResult

interface ReadGroupChatResult

interface RequestTokenSetResult

interface VerifyEmailAddressResult

interface ResetPasswordResult

interface UpdateAccountResult

interface CreateAccountResult

interface EmailEmailAddressVerificationResult

interface CreateGroupChatResult

interface CreatePrivateChatResult

interface CreateTextMessageResult

interface CreateActionMessageResult

interface CreateGroupChatInviteMessageResult

interface CreatePollMessageResult

interface ForwardMessageResult

interface TriggerActionResult

interface SetPollVoteResult

interface LeaveGroupChatResult

data class MessageEdges(val edges: List<MessageEdge>) : SearchChatMessagesResult
