package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.IncorrectPasswordException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.NonexistentUserException
import com.neelkamath.omniChat.graphql.UnverifiedEmailAddressException
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.executeGraphQlViaHttp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

const val READ_STARS_QUERY = """
    query ReadStars {
        readStars {
            $STARRED_MESSAGE_FRAGMENT
        }
    }
"""

private fun operateReadStars(userId: Int): GraphQlResponse = executeGraphQlViaEngine(READ_STARS_QUERY, userId = userId)

fun readStars(userId: Int): List<StarredMessage> {
    val data = operateReadStars(userId).data!!["readStars"] as List<*>
    return objectMapper.convertValue(data)
}

const val READ_ONLINE_STATUSES_QUERY = """
    query ReadOnlineStatuses {
        readOnlineStatuses {
            $ONLINE_STATUS_FRAGMENT
        }
    }
"""

private fun operateReadOnlineStatuses(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(READ_ONLINE_STATUSES_QUERY, userId = userId)

fun readOnlineStatuses(userId: Int): List<OnlineStatus> {
    val data = operateReadOnlineStatuses(userId).data!!["readOnlineStatuses"] as List<*>
    return objectMapper.convertValue(data)
}

const val CAN_DELETE_ACCOUNT_QUERY = """
    query CanDeleteAccount {
        canDeleteAccount
    }
"""

private fun operateCanDeleteAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(CAN_DELETE_ACCOUNT_QUERY, userId = userId)

fun canDeleteAccount(userId: Int): Boolean = operateCanDeleteAccount(userId).data!!["canDeleteAccount"] as Boolean

const val IS_EMAIL_ADDRESS_TAKEN_QUERY = """
    query IsEmailAddressTaken(${"$"}emailAddress: String!) {
        isEmailAddressTaken(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateIsEmailAddressTaken(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(IS_EMAIL_ADDRESS_TAKEN_QUERY, mapOf("emailAddress" to emailAddress))

fun isEmailAddressTaken(emailAddress: String): Boolean =
    operateIsEmailAddressTaken(emailAddress).data!!["isEmailAddressTaken"] as Boolean

const val IS_USERNAME_TAKEN_QUERY = """
    query IsUsernameTaken(${"$"}username: Username!) {
        isUsernameTaken(username: ${"$"}username)
    }
"""

private fun operateIsUsernameTaken(username: Username): GraphQlResponse =
    executeGraphQlViaEngine(IS_USERNAME_TAKEN_QUERY, mapOf("username" to username))

fun isUsernameTaken(username: Username): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

const val READ_ACCOUNT_QUERY = """
    query ReadAccount {
        readAccount {
            $ACCOUNT_FRAGMENT
        }
    }
"""

private fun operateReadAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(READ_ACCOUNT_QUERY, userId = userId)

fun readAccount(userId: Int): Account {
    val data = operateReadAccount(userId).data!!["readAccount"] as Map<*, *>
    return objectMapper.convertValue(data)
}

const val READ_CHATS_QUERY = """
    query ReadChats(
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChats {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChats(
    userId: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    READ_CHATS_QUERY,
    mapOf(
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun readChats(
    userId: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateReadChats(
        userId,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["readChats"] as List<*>
    return objectMapper.convertValue(chats)
}

const val READ_CHAT_QUERY = """
    query ReadChat(
        ${"$"}id: Int!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChat(id: ${"$"}id) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChat(
    userId: Int,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    READ_CHAT_QUERY,
    mapOf(
        "id" to id,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun readChat(
    userId: Int,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): Chat {
    val data = operateReadChat(
        userId,
        id,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(
    userId: Int,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): String = operateReadChat(
    userId,
    id,
    privateChatMessagesPagination,
    usersPagination,
    groupChatMessagesPagination
).errors!![0].message

const val READ_CONTACTS_QUERY = """
    query ReadContacts(${"$"}first: Int, ${"$"}after: Cursor) {
        readContacts(first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateReadContacts(userId: Int, pagination: ForwardPagination? = null): GraphQlResponse =
    executeGraphQlViaEngine(
        READ_CONTACTS_QUERY,
        mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId
    )

fun readContacts(userId: Int, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateReadContacts(userId, pagination).data!!["readContacts"] as Map<*, *>
    return objectMapper.convertValue(data)
}

const val REFRESH_TOKEN_SET_QUERY = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRefreshTokenSet(refreshToken: String): GraphQlResponse =
    executeGraphQlViaEngine(REFRESH_TOKEN_SET_QUERY, mapOf("refreshToken" to refreshToken))

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = operateRefreshTokenSet(refreshToken).data!!["refreshTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

const val REQUEST_TOKEN_SET_QUERY = """
    query RequestTokenSet(${"$"}login: Login!) {
        requestTokenSet(login: ${"$"}login) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRequestTokenSet(login: Login): GraphQlResponse =
    executeGraphQlViaEngine(REQUEST_TOKEN_SET_QUERY, mapOf("login" to login))

fun requestTokenSet(login: Login): TokenSet {
    val data = operateRequestTokenSet(login).data!!["requestTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errRequestTokenSet(login: Login): String = operateRequestTokenSet(login).errors!![0].message

const val SEARCH_CHAT_MESSAGES_QUERY = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query, last: ${"$"}last, before: ${"$"}before) {
            $MESSAGE_EDGE_FRAGMENT
        }
    }
"""

private fun operateSearchChatMessages(
    userId: Int,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CHAT_MESSAGES_QUERY,
    mapOf(
        "chatId" to chatId,
        "query" to query,
        "last" to pagination?.last,
        "before" to pagination?.before?.toString()
    ),
    userId
)

fun searchChatMessages(
    userId: Int,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): List<MessageEdge> {
    val data =
        operateSearchChatMessages(userId, chatId, query, pagination).data!!["searchChatMessages"] as List<*>
    return objectMapper.convertValue(data)
}

fun errSearchChatMessages(
    userId: Int,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): String = operateSearchChatMessages(userId, chatId, query, pagination).errors!![0].message

const val SEARCH_CHATS_QUERY = """
    query SearchChats(
        ${"$"}query: String!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchChats(query: ${"$"}query) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(
    userId: Int,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CHATS_QUERY,
    mapOf(
        "query" to query,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId = userId
)

fun searchChats(
    userId: Int,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateSearchChats(
        userId,
        query,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchChats"] as List<*>
    return objectMapper.convertValue(chats)
}

const val SEARCH_CONTACTS_QUERY = """
    query SearchContacts(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchContacts(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchContacts(
    userId: Int,
    query: String,
    pagination: ForwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CONTACTS_QUERY,
    mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
    userId
)

fun searchContacts(userId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchContacts(userId, query, pagination).data!!["searchContacts"] as Map<*, *>
    return objectMapper.convertValue(data)
}

const val SEARCH_MESSAGES_QUERY = """
    query SearchMessages(
        ${"$"}query: String!
        ${"$"}chatMessages_messages_last: Int
        ${"$"}chatMessages_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchMessages(query: ${"$"}query) {
            $CHAT_MESSAGES_FRAGMENT
        }
    }
"""

private fun operateSearchMessages(
    userId: Int,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_MESSAGES_QUERY,
    mapOf(
        "query" to query,
        "chatMessages_messages_last" to chatMessagesPagination?.last,
        "chatMessages_messages_before" to chatMessagesPagination?.before?.toString(),
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun searchMessages(
    userId: Int,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<ChatMessages> {
    val messages = operateSearchMessages(
        userId,
        query,
        chatMessagesPagination,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchMessages"] as List<*>
    return objectMapper.convertValue(messages)
}

const val SEARCH_USERS_QUERY = """
    query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchUsers(query: String, pagination: ForwardPagination? = null): GraphQlResponse =
    executeGraphQlViaEngine(
        SEARCH_USERS_QUERY,
        mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString())
    )

fun searchUsers(query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchUsers(query, pagination).data!!["searchUsers"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class ChatMessagesDtoTest : FunSpec({
    context("getMessages(DataFetchingEnvironment") {
        /** Data on a group chat having only ever contained an admin. */
        data class AdminMessages(
            /** The ID of the chat's admin. */
            val adminId: Int,
            /** Every message sent has this text. */
            val text: TextMessage,
            /** The ten messages the admin sent. */
            val messageIdList: List<Int>
        )

        fun createUtilizedChat(): AdminMessages {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message = TextMessage("t")
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, message) }
            return AdminMessages(adminId, message, messageIdList)
        }

        fun testPagination(shouldDeleteMessage: Boolean) {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val index = 5
            if (shouldDeleteMessage) Messages.delete(messageIdList[index])
            val last = 3
            searchMessages(
                adminId,
                queryText.value,
                chatMessagesPagination = BackwardPagination(last, before = messageIdList[index])
            ).flatMap { it.messages }.map { it.cursor } shouldBe messageIdList.take(index).takeLast(last)
        }

        test("Messages should paginate using a cursor from a deleted message as if the message still exists") {
            testPagination(shouldDeleteMessage = true)
        }

        test("Only the messages specified by the cursor and limit should be retrieved") {
            testPagination(shouldDeleteMessage = false)
        }

        test("If neither cursor nor limit are supplied, every message should be retrieved") {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            searchMessages(adminId, queryText.value).flatMap { it.messages }.map { it.cursor } shouldBe messageIdList
        }
    }
})

class QueriesTest : FunSpec({
    context("readStars(DataFetchingEnvironment)") {
        test("Only the user's starred messages should be read") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (message1Id, message2Id) = (1..3).map { Messages.message(user1Id, chatId) }
            listOf(message1Id, message2Id).forEach { Stargazers.create(user1Id, it) }
            readStars(user1Id) shouldBe listOf(message1Id, message2Id).map { StarredMessage.build(it) }
        }
    }

    context("readOnlineStatuses(DataFetchingEnvironment)") {
        test("Reading online statuses should only retrieve users the user has in their contacts, or has a chat with") {
            val (contactOwnerId, contactId, chatSharerId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(contactOwnerId, setOf(contactId))
            PrivateChats.create(contactOwnerId, chatSharerId)
            readOnlineStatuses(contactOwnerId).map { it.userId } shouldContainExactlyInAnyOrder
                    listOf(contactId, chatSharerId)
        }
    }

    context("canDeleteAccount(DataFetchingEnvironment)") {
        test("An account should be deletable if the user is the admin of an otherwise empty chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(listOf(adminId))
            canDeleteAccount(adminId).shouldBeTrue()
        }

        test("An account shouldn't be deletable if the user is the last admin of a group chat with other users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            canDeleteAccount(adminId).shouldBeFalse()
        }
    }

    context("isEmailAddressTaken(DataFetchingEnvironment)") {
        test("The email shouldn't be taken") { isEmailAddressTaken("username@example.com").shouldBeFalse() }

        test("The email should be taken") {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            isEmailAddressTaken(address).shouldBeTrue()
        }
    }

    context("isUsernameTaken(DataFetchingEnvironment)") {
        test("The username shouldn't be taken") { isUsernameTaken(Username("username")).shouldBeFalse() }

        test("The username should be taken") {
            val username = createVerifiedUsers(1)[0].info.username
            isUsernameTaken(username).shouldBeTrue()
        }
    }

    context("readAccount(DataFetchingEnvironment)") {
        test("The user's account info should be returned") {
            val user = createVerifiedUsers(1)[0].info
            readAccount(user.id) shouldBe user
        }
    }

    context("readChats(DataFetchingEnvironment)") {
        test("Private chats deleted by the user should be retrieved only for the other user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            readChats(user1Id).shouldBeEmpty()
            readChats(user2Id).shouldNotBeEmpty()
        }

        test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.READ_CHATS) }

        test("Group chat users should be paginated") {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
        }
    }

    context("readChat(DataFetchingEnvironment)") {
        test("The private chat the user just deleted should be read") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            readChat(user1Id, chatId)
        }

        test("Requesting a chat using an invalid ID should return an error") {
            val userId = createVerifiedUsers(1)[0].info.id
            errReadChat(userId, id = 1) shouldBe InvalidChatIdException.message
        }

        test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.READ_CHAT) }

        test("Group chat users should be paginated") {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHAT)
        }
    }

    context("readContacts(DataFetchingEnvironment)") {
        test("Contacts should be read") {
            val (owner, contact1, contact2) = createVerifiedUsers(3).map { it.info }
            Contacts.create(owner.id, setOf(contact1.id, contact2.id))
            readContacts(owner.id).edges.map { it.node } shouldBe listOf(contact1, contact2)
        }

        test("Contacts should be paginated") { testContactsPagination(ContactsOperationName.READ_CONTACTS) }
    }

    context("refreshTokenSet(DataFetchingEnvironment)") {
        test("A refresh token should issue a new token set") {
            val userId = createVerifiedUsers(1)[0].info.id
            val refreshToken = buildAuthToken(userId).refreshToken
            refreshTokenSet(refreshToken)
        }

        test("An invalid refresh token should return an authorization error") {
            val variables = mapOf("refreshToken" to "invalid token")
            executeGraphQlViaHttp(
                REFRESH_TOKEN_SET_QUERY,
                variables
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("requestTokenSet(DataFetchingEnvironment") {
        test("The access token should work") {
            val login = createVerifiedUsers(1)[0].login
            val token = requestTokenSet(login).accessToken
            executeGraphQlViaHttp(
                READ_ACCOUNT_QUERY,
                accessToken = token
            ).shouldNotHaveUnauthorizedStatus()
        }

        test("A token set shouldn't be created for a nonexistent user") {
            val login = Login(Username("username"), Password("password"))
            errRequestTokenSet(login) shouldBe NonexistentUserException.message
        }

        test("A token set shouldn't be created for a user who hasn't verified their email") {
            val login = Login(Username("username"), Password("password"))
            createUser(AccountInput(login.username, login.password, "username@example.com"))
            errRequestTokenSet(login) shouldBe UnverifiedEmailAddressException.message
        }

        test("A token set shouldn't be created for an incorrect password") {
            val login = createVerifiedUsers(1)[0].login
            val invalidLogin = login.copy(password = Password("incorrect password"))
            errRequestTokenSet(invalidLogin) shouldBe IncorrectPasswordException.message
        }
    }

    context("searchChatMessages(DataFetchingEnvironment)") {
        test("Messages should be searched case-insensitively") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId, TextMessage("Hey!"))
            Messages.create(user2Id, chatId, TextMessage(":) hey"))
            Messages.create(user1Id, chatId, TextMessage("How are you?"))
            searchChatMessages(user1Id, chatId, "hey") shouldBe Messages.readPrivateChat(user1Id, chatId).dropLast(1)
        }

        test("Searching in a chat the user isn't in should return an error") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user2Id, user3Id)
            errSearchChatMessages(user1Id, chatId, "query") shouldBe InvalidChatIdException.message
        }

        test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_CHAT_MESSAGES) }
    }

    context("searchChats(DataFetchingEnvironment)") {
        test("Searching a private chat the user deleted shouldn't include the chat in the search results") {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val chatId = PrivateChats.create(user1.id, user2.id)
            PrivateChatDeletions.create(chatId, user1.id)
            searchChats(user1.id, user2.username.value).shouldBeEmpty()
        }

        test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_CHATS) }

        test("Group chat users should be paginated") {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
        }
    }

    context("searchContacts(DataFetchingEnvironment)") {
        test("Contacts should be searched case-insensitively") {
            val accounts = listOf(
                AccountInput(Username("john_doe"), Password("p"), emailAddress = "john.doe@example.com"),
                AccountInput(Username("john_roger"), Password("p"), emailAddress = "john.roger@example.com"),
                AccountInput(Username("nick_bostrom"), Password("p"), emailAddress = "nick.bostrom@example.com"),
                AccountInput(
                    Username("iron_man"),
                    Password("p"),
                    emailAddress = "roger@example.com",
                    firstName = "John"
                )
            ).map {
                createUser(it)
                it.toAccount()
            }
            val userId = createVerifiedUsers(1)[0].info.id
            Contacts.create(userId, accounts.map { it.id }.toSet())
            val testContacts = { query: String, accountList: List<Account> ->
                searchContacts(userId, query).edges.map { it.node } shouldBe accountList
            }
            testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
            testContacts("bost", listOf(accounts[2]))
            testContacts("Roger", listOf(accounts[1], accounts[3]))
        }

        test("Contacts should be paginated") { testContactsPagination(ContactsOperationName.SEARCH_CONTACTS) }
    }

    context("searchMessages(DataFetchingEnvironment)") {
        test(
            """
            Given a user who created a private chat, sent a message, and deleted the chat,
            when searching for the message,
            then it shouldn't be retrieved
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val text = "text"
            Messages.message(user1Id, chatId, TextMessage(text))
            PrivateChatDeletions.create(chatId, user1Id)
            searchMessages(user1Id, text).shouldBeEmpty()
        }

        test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_MESSAGES) }

        test("Group chat users should be paginated") {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
        }
    }

    context("searchUsers(DataFetchingEnvironment)") {
        test("Users should be searched") {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "tony@example.com"),
                AccountInput(Username("iron_fist"), Password("p"), "iron_fist@example.com"),
                AccountInput(Username("hulk"), Password("p"), "bruce@example.com")
            ).map {
                createUser(it)
                it.toAccount()
            }
            searchUsers("iron").edges.map { it.node } shouldContainExactlyInAnyOrder accounts.dropLast(1)
        }

        fun createAccounts(): List<AccountInput> {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "iron.man@example.com"),
                AccountInput(Username("tony_hawk"), Password("p"), "tony.hawk@example.com"),
                AccountInput(Username("lol"), Password("p"), "iron.fist@example.com"),
                AccountInput(Username("another_one"), Password("p"), "another_one@example.com"),
                AccountInput(Username("jo_mama"), Password("p"), "mama@example.com", firstName = "Iron"),
                AccountInput(Username("nope"), Password("p"), "nope@example.com", lastName = "Irony"),
                AccountInput(Username("black_widow"), Password("p"), "black.widow@example.com"),
                AccountInput(Username("iron_spider"), Password("p"), "iron.spider@example.com")
            )
            accounts.forEach(::createUser)
            return accounts
        }

        test("Users should be paginated") {
            val infoCursors = createAccounts()
                .zip(Users.read())
                .map { (newAccount, cursor) -> AccountEdge(newAccount.toAccount(), cursor) }
            val searchedUsers = listOf(infoCursors[0], infoCursors[2], infoCursors[4], infoCursors[5], infoCursors[7])
            val first = 3
            val index = 0
            searchUsers("iron", ForwardPagination(first, infoCursors[index].cursor)).edges shouldBe
                    searchedUsers.subList(index + 1, index + 1 + first)
        }
    }
})

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
    val adminId = createVerifiedUsers(1)[0].info.id
    val chat = GroupChatInput(
        GroupChatTitle("T"),
        GroupChatDescription(""),
        userIdList = listOf(adminId),
        adminIdList = listOf(adminId),
        isBroadcast = false
    )
    val chatId = GroupChats.create(chat)
    val text = TextMessage("t")
    val messageIdList = (1..10).map { Messages.message(adminId, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    when (operation) {
        MessagesOperationName.SEARCH_CHAT_MESSAGES -> searchChatMessages(adminId, chatId, text.value, pagination)
        MessagesOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text.value, chatMessagesPagination = pagination).flatMap { it.messages }
        MessagesOperationName.READ_CHATS ->
            readChats(adminId, groupChatMessagesPagination = pagination)[0].messages.edges
        MessagesOperationName.READ_CHAT ->
            readChat(adminId, chatId, groupChatMessagesPagination = pagination).messages.edges
        MessagesOperationName.SEARCH_CHATS ->
            searchChats(adminId, chat.title.value, groupChatMessagesPagination = pagination)[0].messages.edges
    }.map { it.cursor } shouldBe messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last)
}

fun testContactsPagination(operation: ContactsOperationName) {
    val ownerId = createVerifiedUsers(1)[0].info.id
    val userIdList = createVerifiedUsers(10)
    Contacts.create(ownerId, userIdList.map { it.info.id }.toSet())
    val index = 5
    val cursor = readContacts(ownerId).edges[index].cursor
    val first = 3
    when (operation) {
        ContactsOperationName.READ_CONTACTS -> readContacts(ownerId, ForwardPagination(first, cursor))
        ContactsOperationName.SEARCH_CONTACTS ->
            searchContacts(ownerId, query = "username", pagination = ForwardPagination(first, cursor))
    }.edges.map { it.node } shouldBe userIdList.subList(index + 1, index + 1 + first).map { it.info }
}

fun testGroupChatUsersPagination(operationName: GroupChatUsersOperationName) {
    val adminId = createVerifiedUsers(1)[0].info.id
    val users = createVerifiedUsers(10)
    val userIdList = users.map { it.info.id }
    val groupChat = GroupChatInput(
        GroupChatTitle("T"),
        GroupChatDescription(""),
        userIdList = userIdList + adminId,
        adminIdList = listOf(adminId),
        isBroadcast = false
    )
    val chatId = GroupChats.create(groupChat)
    val text = "text"
    Messages.create(adminId, chatId, TextMessage(text))
    val first = 3
    val userCursors = GroupChatUsers.read()
    val index = 5
    val pagination = ForwardPagination(first, after = userCursors[index])
    val chat = when (operationName) {
        GroupChatUsersOperationName.READ_CHAT -> readChat(adminId, chatId, usersPagination = pagination)
        GroupChatUsersOperationName.READ_CHATS -> readChats(adminId, usersPagination = pagination)[0]
        GroupChatUsersOperationName.SEARCH_CHATS ->
            searchChats(adminId, groupChat.title.value, usersPagination = pagination)[0]
        GroupChatUsersOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text, usersPagination = pagination)[0].chat
    } as GroupChat
    chat.users.edges.map { it.cursor } shouldBe userCursors.subList(index + 1, index + 1 + first).map { it }
}