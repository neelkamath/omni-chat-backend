package com.neelkamath.omniChatBackend.graphql.operations

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChatBackend.buildTokenSet
import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.parseArgument
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment
import java.util.*

interface ChatDto {
    val id: Int

    fun getMessages(env: DataFetchingEnvironment): MessagesConnection
}

/** The chat as seen by the [userId], or an anonymous user if there's no [userId]. */
class GroupChatDto(chatId: Int, private val userId: Int? = null) : ChatDto, ReadChatResult {
    override val id: Int = chatId

    val title: GroupChatTitle

    val description: GroupChatDescription

    val adminIdList: List<Int> = GroupChatUsers.readAdminIdList(id).toList()

    val inviteCode: UUID?

    val isBroadcast: Boolean
    val publicity: GroupChatPublicity

    init {
        val chat = GroupChats.readChat(
            id,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0),
            userId,
        )
        title = chat.title
        description = chat.description
        isBroadcast = chat.isBroadcast
        publicity = chat.publicity
        inviteCode = chat.inviteCode
    }

    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChatUsers.readUsers(id, ForwardPagination(env.getArgument("first"), env.getArgument("after")))

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readGroupChatConnection(id, pagination, userId)
    }
}

class PrivateChatDto(chatId: Int) : ChatDto, ReadChatResult {
    override val id: Int = chatId

    fun getUser(env: DataFetchingEnvironment): Account =
        PrivateChats.read(id, env.userId!!, BackwardPagination(last = 0)).user

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readPrivateChatConnection(id, env.userId!!, pagination)
    }
}

class ChatMessagesEdgeDto(chat: ChatDto, messageEdges: List<MessageEdge>) {
    val node: ChatMessagesDto = ChatMessagesDto(chat, messageEdges)
    val cursor: Cursor = chat.id
}

class ChatMessagesDto(val chat: ChatDto, private val messageEdges: List<MessageEdge>) {
    fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> {
        val last = env.getArgument<Int?>("last")
        val before = env.getArgument<Int?>("before")
        val edges = if (before == null) messageEdges else messageEdges.filter { it.cursor < before }
        return edges.takeLast(last ?: messageEdges.size)
    }
}

class GroupChatInfoDto(private val inviteCode: UUID) : ReadGroupChatResult {
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

    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChats.readChatInfo(inviteCode, ForwardPagination(env.getArgument("first"), env.getArgument("after"))).users
}

data class ChatMessagesConnectionDto(val edges: List<ChatMessagesEdgeDto>, val pageInfo: PageInfo)

data class ChatsConnectionDto(val edges: List<ChatEdgeDto>, val pageInfo: PageInfo)

data class ChatEdgeDto(val node: ChatDto, val cursor: Cursor)

data class GroupChatsConnectionDto(val edges: List<GroupChatEdgeDto>, val pageInfo: PageInfo)

data class GroupChatEdgeDto(val node: GroupChatDto, val cursor: Cursor)

fun readOnlineStatus(env: DataFetchingEnvironment): ReadOnlineStatusResult {
    val userId = env.getArgument<Int>("userId")
    return if (Users.isExisting(userId)) Users.readOnlineStatus(userId) else InvalidUserId
}

fun readTypingUsers(env: DataFetchingEnvironment): List<TypingUsers> {
    env.verifyAuth()
    val idList = PrivateChats.readUserChatIdList(env.userId!!) + GroupChatUsers.readChatIdList(env.userId!!)
    return TypingStatuses.readChats(idList, env.userId!!).toList()
}

fun readAccount(env: DataFetchingEnvironment): Account {
    env.verifyAuth()
    return Users.read(env.userId!!).toAccount()
}

fun readChat(env: DataFetchingEnvironment): ReadChatResult {
    val chatId = env.getArgument<Int>("id")
    if (env.userId == null && GroupChats.isExistentPublicChat(chatId)) return GroupChatDto(chatId)
    env.verifyAuth()
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> PrivateChatDto(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> GroupChatDto(chatId, env.userId!!)
        else -> InvalidChatId
    }
}

fun readChats(env: DataFetchingEnvironment): ChatsConnectionDto {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val chats = GroupChatUsers.readChatIdList(env.userId!!).map { GroupChatDto(it, env.userId!!) } +
            PrivateChats.readUserChatIdList(env.userId!!).map(::PrivateChatDto)
    return buildChatsConnection(chats.toSet(), pagination)
}

/** Builds the [ChatsConnectionDto] for [readChats]. */
@Suppress("DuplicatedCode")
private fun buildChatsConnection(chats: Set<ChatDto>, pagination: ForwardPagination): ChatsConnectionDto {
    val sorted = chats.sortedBy { it.id }
    var edges = sorted
    if (pagination.after != null) edges = edges.filter { it.id > pagination.after }
    if (pagination.first != null) edges = edges.take(pagination.first)
    val startCursor = sorted.firstOrNull()?.id
    val endCursor = sorted.lastOrNull()?.id
    val hasNextPage = when {
        sorted.isEmpty() -> false
        edges.isNotEmpty() -> endCursor!! > edges.last().id
        edges.isEmpty() -> if (pagination.after == null) true else pagination.after < sorted.last().id
        else -> throw NoWhenBranchMatchedException()
    }
    val hasPreviousPage = when {
        sorted.isEmpty() -> false
        edges.isNotEmpty() -> startCursor!! < edges.first().id
        edges.isEmpty() -> if (pagination.after == null) false else pagination.after > sorted.first().id
        else -> throw NoWhenBranchMatchedException()
    }
    val pageInfo = PageInfo(hasNextPage, hasPreviousPage, startCursor, endCursor)
    return ChatsConnectionDto(edges.map { ChatEdgeDto(node = it, cursor = it.id) }, pageInfo)
}

private fun buildGroupChatsConnection(chats: Set<ChatDto>, pagination: ForwardPagination): GroupChatsConnectionDto {
    val (chatEdges, pageInfo) = buildChatsConnection(chats.toSet(), pagination)
    val edges = chatEdges.map { GroupChatEdgeDto(it.node as GroupChatDto, it.cursor) }
    return GroupChatsConnectionDto(edges, pageInfo)
}

fun readContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.read(env.userId!!, pagination)
}

fun requestTokenSet(env: DataFetchingEnvironment): RequestTokenSetResult {
    val login = env.parseArgument<Login>("login")
    if (!Users.isUsernameTaken(login.username)) return NonexistentUser
    val userId = Users.read(login.username).id
    if (!Users.read(userId).hasVerifiedEmailAddress) return UnverifiedEmailAddress
    if (!Users.isValidLogin(login)) return IncorrectPassword
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

fun searchChatMessages(env: DataFetchingEnvironment): SearchChatMessagesResult {
    val chatId = env.getArgument<Int>("chatId")
    val query = env.getArgument<String>("query")
    val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    val edges = if (env.userId == null && GroupChats.isExistentPublicChat(chatId))
        Messages.searchGroupChat(chatId, query, pagination).toList()
    else {
        env.verifyAuth()
        when (chatId) {
            in PrivateChats.readIdList(env.userId!!) ->
                Messages.searchPrivateChat(chatId, env.userId!!, query, pagination).toList()
            in GroupChatUsers.readChatIdList(env.userId!!) ->
                Messages.searchGroupChat(chatId, query, pagination, env.userId!!).toList()
            else -> return InvalidChatId
        }
    }
    return MessageEdges(edges)
}

fun searchChats(env: DataFetchingEnvironment): ChatsConnectionDto {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val groupChats = GroupChats
        .search(
            env.userId!!,
            query,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0),
        )
        .map { GroupChatDto(it.id, env.userId!!) }
    val privateChats =
        PrivateChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { PrivateChatDto(it.id) }
    return buildChatsConnection(groupChats.plus(privateChats).toSet(), pagination)
}

fun readStars(env: DataFetchingEnvironment): StarredMessagesConnectionDto {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val messageCursors = Stargazers.readMessageCursors(env.userId!!, pagination)
    return StarredMessagesConnectionDto(messageCursors, pagination)
}

fun searchContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.search(env.userId!!, env.getArgument("query"), pagination)
}

fun searchMessages(env: DataFetchingEnvironment): ChatMessagesConnectionDto {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats = GroupChats
        .queryUserChatEdges(env.userId!!, query)
        .map { ChatMessagesEdgeDto(GroupChatDto(it.chatId, env.userId!!), it.edges.toList()) }
    val privateChats = PrivateChats
        .queryUserChatEdges(env.userId!!, query)
        .map { ChatMessagesEdgeDto(PrivateChatDto(it.chatId), it.edges.toList()) }
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return buildChatMessagesConnection(groupChats.plus(privateChats).toSet(), pagination)
}

@Suppress("DuplicatedCode")
private fun buildChatMessagesConnection(
    chats: Set<ChatMessagesEdgeDto>,
    pagination: ForwardPagination,
): ChatMessagesConnectionDto {
    val sorted = chats.sortedBy { it.node.chat.id }
    var edges = sorted
    if (pagination.after != null) edges = edges.filter { it.cursor > pagination.after }
    if (pagination.first != null) edges = edges.take(pagination.first)
    val startCursor = if (sorted.isEmpty()) null else sorted[0].cursor
    val endCursor = if (sorted.isEmpty()) null else sorted.last().cursor
    val hasNextPage = when {
        endCursor == null -> false
        edges.isNotEmpty() -> edges.last().cursor < endCursor
        pagination.after == null -> true
        else -> pagination.after < endCursor
    }
    val hasPreviousPage = when {
        startCursor == null -> false
        edges.isNotEmpty() -> edges[0].cursor > startCursor
        pagination.after == null -> false
        else -> pagination.after > startCursor
    }
    return ChatMessagesConnectionDto(edges, PageInfo(hasNextPage, hasPreviousPage, startCursor, endCursor))
}

fun searchUsers(env: DataFetchingEnvironment): AccountsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Users.search(query, pagination)
}

fun searchBlockedUsers(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return BlockedUsers.search(env.userId!!, query, pagination)
}

fun readBlockedUsers(env: DataFetchingEnvironment): AccountsConnectionDto {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val blockedUsers = BlockedUsers.readBlockedUsers(env.userId!!, pagination)
    return AccountsConnectionDto(blockedUsers, pagination)
}

fun readGroupChat(env: DataFetchingEnvironment): ReadGroupChatResult {
    val inviteCode = env.getArgument<UUID>("inviteCode")
    return if (GroupChats.isExistentInviteCode(inviteCode)) GroupChatInfoDto(inviteCode) else InvalidInviteCode
}

fun searchPublicChats(env: DataFetchingEnvironment): GroupChatsConnectionDto {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val chats = GroupChats.searchPublicChats(
        query,
        usersPagination = ForwardPagination(first = 0),
        messagesPagination = BackwardPagination(last = 0),
    ).map { GroupChatDto(it.id) }
    return buildGroupChatsConnection(chats.toSet(), pagination)
}
