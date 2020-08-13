package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.MessageType
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.readUserById
import org.keycloak.representations.idm.UserRepresentation
import java.time.LocalDateTime
import java.util.*

typealias Cursor = Int

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't lowercase, isn't shorter than 256 characters, or
 * doesn't contain non-whitespace characters.
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

/** An [IllegalArgumentException] will be thrown if the [value] exceeds [Bio.MAX_LENGTH] */
data class Bio(val value: String) {
    init {
        if (value.length > MAX_LENGTH)
            throw IllegalArgumentException("The value ($value) cannot exceed $MAX_LENGTH characters.")
    }

    companion object {
        const val MAX_LENGTH = 2500
    }
}

/** An [IllegalArgumentException] will be thrown if the [value] doesn't contain non-whitespace characters. */
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

data class AccountInput(
    val username: Username,
    val password: Password,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
)

interface AccountData {
    val id: Int
    val username: Username
    val emailAddress: String
    val firstName: String?
    val lastName: String?
    val bio: Bio?
}

data class Account(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData {
    /**
     * Case-insensitively searches for the [query] in the [UserRepresentation.username], [UserRepresentation.firstName],
     * [UserRepresentation.lastName], and [UserRepresentation.email].
     */
    fun matches(query: String): Boolean =
        listOfNotNull(username.value, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }
}

data class MessageContext(val hasContext: Boolean, val id: Int?)

enum class GroupChatPublicity { NOT_INVITABLE, INVITABLE, PUBLIC }

interface BareMessage {
    val messageId: Int
    val sender: Account
    val dateTimes: MessageDateTimes
    val context: MessageContext
    val isForwarded: Boolean

    fun toNewTextMessage(): NewTextMessage = NewTextMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        TextMessages.read(messageId)
    )

    fun toNewAudioMessage(): NewAudioMessage =
        NewAudioMessage(Messages.readChatFromMessage(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewGroupChatInviteMessage(): NewGroupChatInviteMessage {
        val chatId = Messages.readChatFromMessage(messageId)
        return NewGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toNewDocMessage(): NewDocMessage =
        NewDocMessage(Messages.readChatFromMessage(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewVideoMessage(): NewVideoMessage =
        NewVideoMessage(Messages.readChatFromMessage(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewPicTextMessage(): NewPicMessage = NewPicMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PicMessages.read(messageId).caption
    )

    fun toNewPollMessage(): NewPollMessage = NewPollMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PollMessages.read(messageId)
    )
}

interface ContactsSubscription

data class NewContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: Int): NewContact =
            with(readUserById(userId)) { NewContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

data class UpdatedContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: Int): UpdatedContact =
            with(readUserById(userId)) { UpdatedContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

interface OnlineStatusesSubscription

data class UpdatedOnlineStatus(val userId: Int, val isOnline: Boolean) : OnlineStatusesSubscription

data class OnlineStatus(val userId: Int, val isOnline: Boolean, val lastOnline: LocalDateTime?)

data class DeletedContact(val id: Int) : ContactsSubscription

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
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
    val publicity: GroupChatPublicity
) {
    init {
        if (adminIdList.isEmpty()) throw IllegalArgumentException("There must be at least one admin.")
        if (!userIdList.containsAll(adminIdList))
            throw IllegalArgumentException(
                "The admin ID list ($adminIdList) must be a subset of the user ID list ($userIdList)."
            )
    }
}

interface UpdatedChatsSubscription

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[MessageText.MAX_LENGTH] characters with at least
 * one non-whitespace.
 */
data class MessageText(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                "The text must be 1-$MAX_LENGTH characters, with at least one non-whitespace."
            )
    }

    companion object {
        const val MAX_LENGTH = 10_000
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[GroupChatTitle.MAX_LENGTH] characters, of which
 * at least one isn't whitespace.
 */
data class GroupChatTitle(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                """
                The title ("$value") must be 1-$MAX_LENGTH characters, with at least one 
                non-whitespace character.
                """.trimIndent()
            )
    }

    companion object {
        const val MAX_LENGTH = 70
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't at most [GroupChatDescription.MAX_LENGTH]
 * characters.
 */
data class GroupChatDescription(val value: String) {
    init {
        if (value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                """The description ("$value") must be at most $MAX_LENGTH characters"""
            )
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

/** An [IllegalArgumentException] will be thrown if the [newUsers] and [removedUsers] aren't distinct. */
data class UpdatedGroupChat(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUsers: List<Account>? = null,
    val removedUsers: List<Account>? = null,
    val adminIdList: List<Int>? = null,
    val isBroadcast: Boolean? = null,
    val publicity: GroupChatPublicity? = null
) : UpdatedChatsSubscription {
    init {
        if (newUsers != null && removedUsers != null) {
            val intersection = newUsers.intersect(removedUsers)
            if (intersection.isNotEmpty())
                throw IllegalArgumentException(
                    "The list of new and removed users must be distinct. Users in both lists: $intersection."
                )
        }
    }
}

interface TypingStatusesSubscription

data class TypingStatus(val chatId: Int, val userId: Int, val isTyping: Boolean) : TypingStatusesSubscription

data class UpdatedAccount(
    val userId: Int,
    val username: Username,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
) : UpdatedChatsSubscription {
    companion object {
        fun build(userId: Int): UpdatedAccount =
            with(readUserById(userId)) { UpdatedAccount(userId, username, emailAddress, firstName, lastName, bio) }
    }
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
    override val publicity: GroupChatPublicity
) : BareGroupChat

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
) : Chat, BareGroupChat

data class MessagesConnection(val edges: List<MessageEdge>, val pageInfo: PageInfo)

data class MessageEdge(val node: Message, val cursor: Cursor)

interface BareChatMessage : BareMessage {
    val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
}

interface Message : BareMessage {
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
    val hasStar: Boolean

    fun toUpdatedTextMessage(): UpdatedTextMessage = UpdatedTextMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        TextMessages.read(messageId)
    )

    fun toUpdatedPicMessage(): UpdatedPicMessage = UpdatedPicMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        PicMessages.read(messageId).caption
    )

    fun toUpdatedAudioMessage(): UpdatedAudioMessage = UpdatedAudioMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedGroupChatInviteMessage(): UpdatedGroupChatInviteMessage {
        val chatId = Messages.readChatFromMessage(messageId)
        return UpdatedGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            hasStar,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toUpdatedDocMessage(): UpdatedDocMessage = UpdatedDocMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedVideoMessage(): UpdatedVideoMessage = UpdatedVideoMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedPollMessage(): UpdatedPollMessage = UpdatedPollMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        PollMessages.read(messageId)
    )

    fun toStarredTextMessage(): StarredTextMessage = StarredTextMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        TextMessages.read(messageId)
    )

    fun toStarredPicMessage(): StarredPicMessage = StarredPicMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PicMessages.read(messageId).caption
    )

    fun toStarredAudioMessage(): StarredAudioMessage = StarredAudioMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredGroupChatInviteMessage(): StarredGroupChatInviteMessage {
        val chatId = Messages.readChatFromMessage(messageId)
        return StarredGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toStarredDocMessage(): StarredDocMessage = StarredDocMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredVideoMessage(): StarredVideoMessage = StarredVideoMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredPollMessage(): StarredPollMessage = StarredPollMessage(
        Messages.readChatFromMessage(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PollMessages.read(messageId)
    )
}

data class TextMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: MessageText
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): TextMessage = with(message) {
            TextMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                TextMessages.read(messageId)
            )
        }
    }
}

data class PicMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val caption: MessageText?
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): PicMessage = with(message) {
            PicMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PicMessages.read(messageId).caption
            )
        }
    }
}

data class AudioMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): AudioMessage = with(message) {
            AudioMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class GroupChatInviteMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val inviteCode: UUID
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): GroupChatInviteMessage = with(message) {
            GroupChatInviteMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                GroupChats.readInviteCode(Messages.readChatFromMessage(messageId))
            )
        }
    }
}

data class DocMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): DocMessage = with(message) {
            DocMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class VideoMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): VideoMessage = with(message) {
            VideoMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class PollMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val poll: Poll
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): PollMessage = with(message) {
            PollMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PollMessages.read(messageId)
            )
        }
    }
}

interface StarredMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId] as seen by the [userId]. */
        fun build(userId: Int, messageId: Int): StarredMessage =
            when (val message = Messages.readMessage(userId, messageId)) {
                is TextMessage -> message.toStarredTextMessage()
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
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: MessageText
) : StarredMessage, BareChatMessage, BareMessage

data class StarredPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?
) : StarredMessage, BareChatMessage, BareMessage

data class StarredPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll
) : StarredMessage, BareChatMessage, BareMessage

data class StarredAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

data class StarredGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID
) : StarredMessage, BareChatMessage, BareMessage

data class StarredDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

data class StarredVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

interface MessagesSubscription

interface NewMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId]. */
        fun build(messageId: Int): NewMessage {
            val (type, message) = Messages.readTypedMessage(messageId)
            return when (type) {
                MessageType.TEXT -> message.toNewTextMessage()
                MessageType.PIC -> message.toNewPicTextMessage()
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
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: MessageText
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

interface UpdatedMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
    val hasStar: Boolean

    companion object {
        /** Returns a concrete class for the [messageId] as seen by the [userId]. */
        fun build(userId: Int, messageId: Int): UpdatedMessage =
            when (val message = Messages.readMessage(userId, messageId)) {
                is TextMessage -> message.toUpdatedTextMessage()
                is PicMessage -> message.toUpdatedPicMessage()
                is AudioMessage -> message.toUpdatedAudioMessage()
                is GroupChatInviteMessage -> message.toUpdatedGroupChatInviteMessage()
                is DocMessage -> message.toUpdatedDocMessage()
                is VideoMessage -> message.toUpdatedVideoMessage()
                is PollMessage -> message.toUpdatedPollMessage()
                else -> throw IllegalArgumentException("$message didn't match a concrete class.")
            }
    }
}

data class UpdatedTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: MessageText
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val caption: MessageText?
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val poll: Poll
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val inviteCode: UUID
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class MessageDateTimes(val sent: LocalDateTime, val statuses: List<MessageDateTimeStatus>)

data class MessageDateTimeStatus(val user: Account, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val chatId: Int, val messageId: Int) : MessagesSubscription

data class MessageDeletionPoint(val chatId: Int, val until: LocalDateTime) : MessagesSubscription

data class UserChatMessagesRemoval(val chatId: Int, val userId: Int) : MessagesSubscription

data class ExitedUser(val userId: Int, val chatId: Int) : UpdatedChatsSubscription

interface NewGroupChatsSubscription

data class GroupChatId(val id: Int) : NewGroupChatsSubscription

data class DeletionOfEveryMessage(val chatId: Int) : MessagesSubscription

object CreatedSubscription :
    MessagesSubscription,
    ContactsSubscription,
    UpdatedChatsSubscription,
    NewGroupChatsSubscription,
    TypingStatusesSubscription,
    OnlineStatusesSubscription {

    @Suppress("unused")
    val placeholder = Placeholder
}

data class ChatMessages(val chat: Chat, val messages: List<MessageEdge>)

data class AccountsConnection(val edges: List<AccountEdge>, val pageInfo: PageInfo) {
    companion object {
        /** The [accountEdges] will be sorted in ascending order of their [AccountEdge.cursor] for you. */
        fun build(accountEdges: List<AccountEdge>, pagination: ForwardPagination? = null): AccountsConnection {
            val (first, after) = pagination ?: ForwardPagination()
            val accounts = accountEdges.sortedBy { it.cursor }
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

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
private fun <T> assertOptions(options: List<T>) {
    if (options.size < 2) throw IllegalArgumentException("There must be at least two options: $options.")
    if (options.toSet().size != options.size) throw IllegalArgumentException("Options must be unique: $options.")
}

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class PollInput(val title: MessageText, val options: List<MessageText>) {
    init {
        assertOptions(options)
    }
}

data class PollOption(val option: MessageText, val votes: List<Int>)

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class Poll(val title: MessageText, val options: List<PollOption>) {
    init {
        assertOptions(options)
    }
}