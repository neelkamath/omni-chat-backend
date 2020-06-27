package graphql.operations.queries

import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.chats.GroupChatUsers
import com.neelkamath.omniChat.db.chats.read
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import graphql.operations.mutations.createContacts
import graphql.operations.mutations.createGroupChat
import graphql.operations.mutations.createMessage
import io.kotest.matchers.shouldBe

/** The name of a GraphQL operation which is concerned with the pagination of messages. */
enum class MessagesOperationName {
    /** Represents `Query.searchChatMessages`. */
    SEARCH_CHAT_MESSAGES,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS
}

/** The name of a GraphQL operation which is concerned with the pagination of contacts. */
enum class ContactsOperationName {
    /** Represents `Query.readContacts`. */
    READ_CONTACTS,

    /** Represents `Query.searchContacts`. */
    SEARCH_CONTACTS
}

/** The name of a GraphQL operation which is concerned with the pagination of accounts. */
enum class GroupChatUsersOperationName {
    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES
}

/** Asserts that the [operation] paginates correctly. */
fun testMessagesPagination(operation: MessagesOperationName) {
    val adminToken = createSignedInUsers(1)[0].accessToken
    val title = "Title"
    val chatId = createGroupChat(adminToken, NewGroupChat(title))
    val text = "text"
    val messageIdList = (1..10).map { messageAndReadId(adminToken, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    when (operation) {
        MessagesOperationName.SEARCH_CHAT_MESSAGES -> searchChatMessages(adminToken, chatId, text, pagination)
        MessagesOperationName.SEARCH_MESSAGES ->
            searchMessages(adminToken, text, messagesPagination = pagination).flatMap { it.messages }
        MessagesOperationName.READ_CHATS -> readChats(adminToken, messagesPagination = pagination)[0].messages.edges
        MessagesOperationName.READ_CHAT -> readChat(adminToken, chatId, messagesPagination = pagination).messages.edges
        MessagesOperationName.SEARCH_CHATS ->
            searchChats(adminToken, title, messagesPagination = pagination)[0].messages.edges
    }.map { it.cursor } shouldBe messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last)
}

fun testContactsPagination(operation: ContactsOperationName) {
    val owner = createSignedInUsers(1)[0]
    val userIdList = createSignedInUsers(10)
    createContacts(owner.accessToken, userIdList.map { it.info.id })
    val index = 5
    val cursor = readContacts(owner.accessToken).edges[index].cursor
    val first = 3
    when (operation) {
        ContactsOperationName.READ_CONTACTS ->
            readContacts(owner.accessToken, ForwardPagination(first, cursor))

        ContactsOperationName.SEARCH_CONTACTS -> searchContacts(
            owner.accessToken,
            query = "username",
            pagination = ForwardPagination(first, cursor)
        )
    }.edges.map { it.node } shouldBe userIdList.subList(index + 1, index + 1 + first).map { it.info }
}

fun testGroupChatUsersPagination(operationName: GroupChatUsersOperationName) {
    val admin = createSignedInUsers(1)[0]
    val users = createSignedInUsers(10)
    val userIdList = users.map { it.info.id }
    val groupChat = NewGroupChat("Title", userIdList = userIdList)
    val chatId = createGroupChat(admin.accessToken, groupChat)
    val text = "text"
    createMessage(admin.accessToken, chatId, text)
    val first = 3
    val userCursors = GroupChatUsers.read()
    val index = 5
    val pagination = ForwardPagination(first, after = userCursors[index])
    val chat = when (operationName) {
        GroupChatUsersOperationName.READ_CHAT -> readChat(admin.accessToken, chatId, pagination)
        GroupChatUsersOperationName.READ_CHATS -> readChats(admin.accessToken, pagination)[0]
        GroupChatUsersOperationName.SEARCH_CHATS -> searchChats(admin.accessToken, groupChat.title, pagination)[0]
        GroupChatUsersOperationName.SEARCH_MESSAGES -> searchMessages(admin.accessToken, text, pagination)[0].chat
    } as GroupChat
    chat.users.edges.map { it.cursor } shouldBe userCursors.subList(index + 1, index + 1 + first).map { it }
}