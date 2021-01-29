package com.neelkamath.omniChat.graphql.operations

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.buildTokenSet
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.userId
import graphql.schema.DataFetchingEnvironment
import java.util.*

interface ChatDto {
    val id: Int

    fun getMessages(env: DataFetchingEnvironment): MessagesConnection
}

/** The chat as seen by the [userId], or an anonymous user if there's no [userId]. */
class GroupChatDto(chatId: Int, private val userId: Int? = null) : ChatDto {
    override val id: Int = chatId

    @Suppress("MemberVisibilityCanBePrivate")
    val title: GroupChatTitle

    @Suppress("MemberVisibilityCanBePrivate")
    val description: GroupChatDescription

    @Suppress("unused")
    val adminIdList: List<Int> = GroupChatUsers.readAdminIdList(id)

    @Suppress("MemberVisibilityCanBePrivate")
    val inviteCode: UUID?

    val isBroadcast: Boolean
    val publicity: GroupChatPublicity

    init {
        val chat = GroupChats.readChat(
            id,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0),
            userId = userId
        )
        title = chat.title
        description = chat.description
        isBroadcast = chat.isBroadcast
        publicity = chat.publicity
        inviteCode = chat.inviteCode
    }

    @Suppress("unused")
    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChatUsers.readUsers(id, ForwardPagination(env.getArgument("first"), env.getArgument("after")))

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readGroupChatConnection(id, pagination, userId)
    }
}

class PrivateChatDto(chatId: Int) : ChatDto {
    override val id: Int = chatId

    @Suppress("unused")
    fun getUser(env: DataFetchingEnvironment): Account =
        PrivateChats.read(id, env.userId!!, BackwardPagination(last = 0)).user

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readPrivateChatConnection(id, env.userId!!, pagination)
    }
}

sealed class ChatMessagesDto(val chat: ChatDto, private val messageEdges: List<MessageEdge>) {
    @Suppress("unused")
    fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> {
        val last = env.getArgument<Int?>("last")
        val before = env.getArgument<Int?>("before")
        val edges = if (before == null) messageEdges else messageEdges.filter { it.cursor < before }
        return edges.takeLast(last ?: messageEdges.size)
    }
}

/** The [userId] searching. */
private class SearchGroupChatMessagesDto(
    userId: Int,
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(GroupChatDto(chatId, userId), messageEdges)

private class SearchPrivateChatMessagesDto(
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(PrivateChatDto(chatId), messageEdges)

class GroupChatInfoDto(private val inviteCode: UUID) {
    val adminIdList: List<Int>
    val title: GroupChatTitle
    val description: GroupChatDescription
    val isBroadcast: Boolean
    val publicity: GroupChatPublicity

    init {
        val info = GroupChats.readChatInfo(inviteCode, usersPagination = ForwardPagination(first = 0))
        adminIdList = info.adminIdList
        title = info.title
        description = info.description
        isBroadcast = info.isBroadcast
        publicity = info.publicity
    }

    @Suppress("unused")
    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChats.readChatInfo(inviteCode, ForwardPagination(env.getArgument("first"), env.getArgument("after"))).users
}

fun canDeleteAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return GroupChatUsers.canUserLeave(env.userId!!)
}

fun readOnlineStatuses(env: DataFetchingEnvironment): List<OnlineStatus> {
    env.verifyAuth()
    val userIdList = Contacts.readIdList(env.userId!!) +
            PrivateChats.readOtherUserIdList(env.userId!!) +
            GroupChatUsers.readFellowParticipants(env.userId!!)
    return userIdList.map {
        with(Users.read(it)) { OnlineStatus(it, isOnline, lastOnline) }
    }
}

fun isEmailAddressTaken(env: DataFetchingEnvironment): Boolean =
    Users.isEmailAddressTaken(env.getArgument("emailAddress"))

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean {
    val username = env.parseArgument<Username>("username")
    return Users.isUsernameTaken(username)
}

fun readAccount(env: DataFetchingEnvironment): Account {
    env.verifyAuth()
    return Users.read(env.userId!!).toAccount()
}

fun readChat(env: DataFetchingEnvironment): ChatDto {
    val chatId = env.getArgument<Int>("id")
    if (env.userId == null && GroupChats.isExistentPublicChat(chatId)) return GroupChatDto(chatId)
    env.verifyAuth()
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> PrivateChatDto(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> GroupChatDto(chatId, env.userId!!)
        else -> throw InvalidChatIdException
    }
}

fun readChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    return GroupChatUsers.readChatIdList(env.userId!!).map { GroupChatDto(it, env.userId!!) } +
            PrivateChats.readUserChatIdList(env.userId!!).map(::PrivateChatDto)
}

fun readContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.read(env.userId!!, pagination)
}

fun requestTokenSet(env: DataFetchingEnvironment): TokenSet {
    val login = env.parseArgument<Login>("login")
    if (!Users.isUsernameTaken(login.username)) throw NonexistentUserException
    val userId = Users.read(login.username).id
    if (!Users.read(userId).hasVerifiedEmailAddress) throw UnverifiedEmailAddressException
    if (!Users.isValidLogin(login)) throw IncorrectPasswordException
    return buildTokenSet(userId)
}

fun refreshTokenSet(env: DataFetchingEnvironment): TokenSet {
    val refreshToken = env.getArgument<String>("refreshToken")
    val userId = try {
        JWT.decode(refreshToken).subject.toInt()
    } catch (_: JWTDecodeException) {
        throw UnauthorizedException
    }
    return buildTokenSet(userId)
}

fun searchChatMessages(env: DataFetchingEnvironment): List<MessageEdge> {
    val chatId = env.getArgument<Int>("chatId")
    val query = env.getArgument<String>("query")
    val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    if (env.userId == null && GroupChats.isExistentPublicChat(chatId))
        return Messages.searchGroupChat(chatId, query, pagination)
    env.verifyAuth()
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> Messages.searchPrivateChat(chatId, env.userId!!, query, pagination)
        in GroupChatUsers.readChatIdList(env.userId!!) ->
            Messages.searchGroupChat(chatId, query, pagination, env.userId!!)
        else -> throw InvalidChatIdException
    }
}

fun searchChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats = GroupChats
        .search(
            env.userId!!,
            query,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0)
        )
        .map { GroupChatDto(it.id, env.userId!!) }
    val privateChats =
        PrivateChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { PrivateChatDto(it.id) }
    return groupChats + privateChats
}

fun readStars(env: DataFetchingEnvironment): List<StarredMessage> {
    env.verifyAuth()
    return Stargazers.read(env.userId!!).map { StarredMessage.build(env.userId!!, it) }
}

fun searchContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.search(env.userId!!, env.getArgument("query"), pagination)
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessagesDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats = GroupChats
        .queryUserChatEdges(env.userId!!, query)
        .map { SearchGroupChatMessagesDto(env.userId!!, it.chatId, it.edges) }
    val privateChats = PrivateChats
        .queryUserChatEdges(env.userId!!, query)
        .map { SearchPrivateChatMessagesDto(it.chatId, it.edges) }
    return groupChats + privateChats
}

fun searchUsers(env: DataFetchingEnvironment): AccountsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Users.search(query, pagination)
}

fun readBlockedUsers(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return BlockedUsers.read(env.userId!!, pagination)
}

fun readGroupChat(env: DataFetchingEnvironment): GroupChatInfoDto {
    val inviteCode = env.getArgument<UUID>("inviteCode")
    if (!GroupChats.isExistentInviteCode(inviteCode)) throw InvalidInviteCodeException
    return GroupChatInfoDto(inviteCode)
}

fun searchPublicChats(env: DataFetchingEnvironment): List<GroupChatDto> {
    val query = env.getArgument<String>("query")
    return GroupChats.searchPublicChats(
        query,
        usersPagination = ForwardPagination(first = 0),
        messagesPagination = BackwardPagination(last = 0)
    ).map { GroupChatDto(it.id) }
}

fun isBlocked(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return BlockedUsers.exists(env.userId!!, env.getArgument("userId"))
}

fun isContact(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return env.getArgument("userId") in Contacts.readIdList(env.userId!!)
}
