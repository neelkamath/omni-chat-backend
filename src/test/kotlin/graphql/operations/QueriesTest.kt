package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.buildTokenSet
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.toLinkedHashSet
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.test.*

@ExtendWith(DbExtension::class)
class ChatMessagesDtoTest {
    @Nested
    inner class ReadGroupChat {
        @Test
        fun `The chat's info must be read`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val inviteCode = GroupChats.readInviteCode(chatId)
            assertEquals(GroupChats.readChatInfo(inviteCode), readGroupChat(inviteCode))
        }

        @Test
        fun `Reading a chat using a nonexistent invite code must fail`(): Unit =
            assertTrue(readGroupChat(UUID.randomUUID()) is InvalidInviteCode)
    }

    /** Data on a group chat having only ever contained an admin. */
    data class AdminMessages(
        /** The ID of the chat's admin. */
        val adminId: Int,
        /** Every message sent has this text. */
        val text: MessageText,
        /** The ten messages the admin sent in the order of their creation. */
        val messageIdList: LinkedHashSet<Int>,
    )

    @Nested
    inner class GetMessages {
        private fun createUtilizedChat(): AdminMessages {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message = MessageText("t")
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, message) }.toLinkedHashSet()
            return AdminMessages(adminId, message, messageIdList)
        }

        private fun testPagination(mustDeleteMessage: Boolean) {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val index = 5
            if (mustDeleteMessage) Messages.delete(messageIdList.elementAt(index))
            val last = 3
            val cursors = searchMessages(
                adminId,
                queryText.value,
                chatMessagesPagination = BackwardPagination(last, before = messageIdList.elementAt(index)),
            ).flatMap { it.messages }.map { it.cursor }
            assertEquals(messageIdList.take(index).takeLast(last), cursors)
        }

        @Test
        fun `Messages must paginate using a cursor from a deleted message as if the message still exists`(): Unit =
            testPagination(mustDeleteMessage = true)

        @Test
        fun `Only the messages specified by the cursor and limit must be retrieved`(): Unit =
            testPagination(mustDeleteMessage = false)

        @Test
        fun `If neither cursor nor limit are supplied, every message must be retrieved`() {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val cursors = searchMessages(adminId, queryText.value).flatMap { it.messages }.map { it.cursor }.toSet()
            assertEquals(messageIdList, cursors)
        }
    }
}

@ExtendWith(DbExtension::class)
class QueriesTest {
    @Nested
    inner class SearchBlockedUsers {
        @Test
        fun `Blocked users must be searched`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            val actual = searchBlockedUsers(blockerId, query = "").edges.map { it.node.id }
            assertEquals(listOf(blockedId), actual)
        }

        @Test
        fun `Results must be paginated`(): Unit =
            testBlockedUsersPagination(BlockedUsersOperationName.SEARCH_BLOCKED_USERS)
    }

    @Nested
    inner class ReadTypingUsers {
        @Test
        fun `Typing statuses from the user's chats must be read excluding the user's own`() {
            val (adminId, participant1Id, participant2Id, nonParticipantId) = createVerifiedUsers(4).map { it.info.id }
            val groupChatId = GroupChats.create(listOf(adminId), listOf(participant1Id, participant2Id))
            val privateChatId = PrivateChats.create(participant2Id, nonParticipantId)
            TypingStatuses.update(groupChatId, adminId, isTyping = true)
            TypingStatuses.update(groupChatId, participant1Id, isTyping = true)
            TypingStatuses.update(privateChatId, nonParticipantId, isTyping = true)
            val users = listOf(Users.read(participant1Id).toAccount())
            val expected = listOf(TypingUsers(groupChatId, users))
            assertEquals(expected, readTypingUsers(adminId))
        }
    }

    @Nested
    inner class ReadBlockedUsers {
        @Test
        fun `Blocked users must be paginated`(): Unit =
            testBlockedUsersPagination(BlockedUsersOperationName.READ_BLOCKED_USERS)
    }

    @Nested
    inner class SearchPublicChats {
        @Test
        fun `Chats must be case-insensitively queried by their title`() {
            val adminId = createVerifiedUsers(1).first().info.id
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Kotlin/Native"))
            val chatId = GroupChats
                .create(listOf(adminId), title = GroupChatTitle("Kotlin/JS"), publicity = GroupChatPublicity.PUBLIC)
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Gaming"), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(listOf(chatId), searchPublicChats("kotlin").map { it.id })
        }
    }

    @Nested
    inner class ReadStars {
        @Test
        fun `Only the user's starred messages must be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (message1Id, message2Id) = (1..3).map { Messages.message(user1Id, chatId) }
            listOf(message1Id, message2Id).forEach { Stargazers.create(user1Id, it) }
            assertEquals(listOf(message1Id, message2Id).map { StarredMessage.build(user1Id, it) }, readStars(user1Id))
        }
    }

    @Nested
    inner class ReadOnlineStatus {
        @Test
        fun `The online status must be read`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertEquals(Users.readOnlineStatus(userId), readOnlineStatus(userId))
        }

        @Test
        fun `Reading the online status of a nonexistent user must fail`(): Unit =
            assertTrue(readOnlineStatus(userId = -1) is InvalidUserId)
    }

    @Nested
    inner class ReadAccount {
        @Test
        fun `The user's account info must be returned`() {
            val user = createVerifiedUsers(1).first().info
            assertEquals(user, readAccount(user.id))
        }
    }

    @Nested
    inner class ReadChats {
        @Test
        fun `Private chats deleted by the user must be retrieved only for the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(readChats(user1Id).isEmpty())
            assertFalse(readChats(user2Id).isEmpty())
        }

        @Test
        fun `Messages must be paginated`() {
            testMessagesPagination(MessagesOperationName.READ_CHATS)
        }

        @Test
        fun `Group chat users must be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
        }
    }

    @Nested
    inner class ReadChat {
        @Test
        fun `The private chat the user just deleted must be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(PrivateChats.read(chatId, user1Id), readChat(chatId, userId = user1Id))
        }

        @Test
        fun `Reading a public chat mustn't require an access token`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(GroupChats.readChat(chatId), readChat(chatId))
        }

        @Test
        fun `When a user reads a public chat, the chat must be represented the way they see it`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val chat = readChat(chatId, userId = adminId) as GroupChat
            assertEquals(listOf(true), chat.messages.edges.map { it.node.hasStar })
        }

        @Test
        fun `Requesting a chat using an invalid ID must return an error`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(readChat(id = 1, userId = userId) is InvalidChatId)
        }

        @Test
        fun `Messages must be paginated`() {
            testMessagesPagination(MessagesOperationName.READ_CHAT)
        }

        @Test
        fun `Group chat users must be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHAT)
        }
    }

    @Nested
    inner class ReadContacts {
        @Test
        fun `Contacts must be read`() {
            val (owner, contact1, contact2) = createVerifiedUsers(3).map { it.info }
            Contacts.createAll(owner.id, setOf(contact1.id, contact2.id))
            assertEquals(listOf(contact1, contact2), readContacts(owner.id).edges.map { it.node })
        }

        @Test
        fun `Contacts must be paginated`() {
            testContactsPagination(ContactsOperationName.READ_CONTACTS)
        }
    }

    @Nested
    inner class RefreshTokenSet {
        @Test
        fun `A refresh token must issue a new token set`() {
            val userId = createVerifiedUsers(1).first().info.id
            val refreshToken = buildTokenSet(userId).refreshToken
            refreshTokenSet(refreshToken)
        }

        @Test
        fun `An invalid refresh token must return an authorization error`() {
            val variables = mapOf("refreshToken" to "invalid token")
            val response = executeGraphQlViaHttp(REFRESH_TOKEN_SET_QUERY, variables)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class RequestTokenSet {
        @Test
        fun `The access token must work`() {
            val login = createVerifiedUsers(1).first().login
            val tokenSet = requestTokenSet(login) as TokenSet
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = tokenSet.accessToken)
            assertNotEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `A nonexistent user must cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            assertTrue(requestTokenSet(login) is NonexistentUser)
        }

        @Test
        fun `A user who hasn't verified their email must cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            Users.create(AccountInput(login.username, login.password, "username@example.com"))
            assertTrue(requestTokenSet(login) is UnverifiedEmailAddress)
        }

        @Test
        fun `An incorrect password must cause an exception to be thrown`() {
            val login = createVerifiedUsers(1).first().login
            val invalidLogin = login.copy(password = Password("incorrect password"))
            assertTrue(requestTokenSet(invalidLogin) is IncorrectPassword)
        }
    }

    @Nested
    inner class SearchChatMessages {
        @Test
        fun `Messages must be searched case-insensitively`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId, MessageText("Hey!"))
            Messages.create(user2Id, chatId, MessageText(":) hey"))
            Messages.create(user1Id, chatId, MessageText("How are you?"))
            val edges = searchChatMessages(chatId, "hey", userId = user1Id) as MessageEdges
            assertEquals(Messages.readPrivateChat(user1Id, chatId).toList().dropLast(1), edges.edges)
        }

        @Test
        fun `Searching in a non-public chat the user isn't in must return an error`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user2Id, user3Id)
            assertTrue(searchChatMessages(chatId, "query", userId = user1Id) is InvalidChatId)
        }

        @Test
        fun `A public chat must be searchable without an account`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val text = "text"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            val edges = searchChatMessages(chatId, text) as MessageEdges
            assertEquals(listOf(messageId), edges.edges.map { it.node.messageId })
        }

        @Test
        fun `When a user searches a public chat, it must be returned as it's seen by the user`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val text = "t"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            Stargazers.create(adminId, messageId)
            val edges = searchChatMessages(chatId, text, userId = adminId) as MessageEdges
            assertEquals(listOf(true), edges.edges.map { it.node.hasStar })
        }

        @Test
        fun `Messages must be paginated`() {
            testMessagesPagination(MessagesOperationName.SEARCH_CHAT_MESSAGES)
        }
    }

    @Nested
    inner class SearchChats {
        @Test
        fun `Searching a private chat the user deleted mustn't include the chat in the search results`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val chatId = PrivateChats.create(user1.id, user2.id)
            PrivateChatDeletions.create(chatId, user1.id)
            assertTrue(searchChats(user1.id, user2.username.value).isEmpty())
        }

        @Test
        fun `Messages must be paginated`() {
            testMessagesPagination(MessagesOperationName.SEARCH_CHATS)
        }

        @Test
        fun `Group chat users must be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
        }
    }

    @Nested
    inner class SearchContacts {
        @Test
        fun `Contacts must be searched case-insensitively`() {
            val accounts = listOf(
                AccountInput(Username("john_doe"), Password("p"), emailAddress = "john.doe@example.com"),
                AccountInput(Username("john_roger"), Password("p"), emailAddress = "john.roger@example.com"),
                AccountInput(Username("nick_bostrom"), Password("p"), emailAddress = "nick.bostrom@example.com"),
                AccountInput(Username("iron_man"), Password("p"), emailAddress = "roger@example.com", Name("John")),
            ).map {
                Users.create(it)
                it.toAccount()
            }
            val userId = createVerifiedUsers(1).first().info.id
            Contacts.createAll(userId, accounts.map { it.id }.toSet())
            val testContacts = { query: String, accountList: List<Account> ->
                assertEquals(accountList, searchContacts(userId, query).edges.map { it.node })
            }
            testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
            testContacts("bost", listOf(accounts[2]))
            testContacts("Roger", listOf(accounts[1], accounts[3]))
        }

        @Test
        fun `Contacts must be paginated`() {
            testContactsPagination(ContactsOperationName.SEARCH_CONTACTS)
        }
    }

    @Nested
    inner class SearchMessages {
        @Test
        fun `Searching for a message sent before the private chat was deleted mustn't be found`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val text = "text"
            Messages.message(user1Id, chatId, MessageText(text))
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(searchMessages(user1Id, text).isEmpty())
        }

        @Test
        fun `Messages must be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
        }
    }

    @Nested
    inner class SearchUsers {
        @Test
        fun `Users must be searched`() {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "tony@example.com"),
                AccountInput(Username("iron_fist"), Password("p"), "iron_fist@example.com"),
                AccountInput(Username("hulk"), Password("p"), "bruce@example.com"),
            ).map {
                Users.create(it)
                it.toAccount()
            }
            assertEquals(accounts.dropLast(1), searchUsers("iron").edges.map { it.node })
        }

        private fun createAccounts(): Set<AccountInput> {
            val accounts = setOf(
                AccountInput(Username("iron_man"), Password("p"), "iron.man@example.com"),
                AccountInput(Username("tony_hawk"), Password("p"), "tony.hawk@example.com"),
                AccountInput(Username("lol"), Password("p"), "iron.fist@example.com"),
                AccountInput(Username("another_one"), Password("p"), "another_one@example.com"),
                AccountInput(Username("jo_mama"), Password("p"), "mama@example.com", firstName = Name("Iron")),
                AccountInput(Username("nope"), Password("p"), "nope@example.com", lastName = Name("Irony")),
                AccountInput(Username("black_widow"), Password("p"), "black.widow@example.com"),
                AccountInput(Username("iron_spider"), Password("p"), "iron.spider@example.com"),
            )
            accounts.forEach(Users::create)
            return accounts
        }

        @Test
        fun `Users must be paginated`() {
            val infoCursors = createAccounts()
                .zip(Users.read())
                .map { (newAccount, cursor) -> AccountEdge(newAccount.toAccount(), cursor) }
            val searchedUsers = listOf(infoCursors[0], infoCursors[2], infoCursors[4], infoCursors[5], infoCursors[7])
            val first = 3
            val index = 0
            val users = searchUsers("iron", ForwardPagination(first, infoCursors[index].cursor)).edges
            assertEquals(searchedUsers.subList(index + 1, index + 1 + first), users)
        }
    }
}

/** The name of a GraphQL operation which is concerned with the pagination of blocked users. */
private enum class BlockedUsersOperationName {
    /** Represents `Query.readBlockedUsers`. */
    READ_BLOCKED_USERS,

    /** Represents `Query.searchBlockedUsers`. */
    SEARCH_BLOCKED_USERS,
}

private fun testBlockedUsersPagination(operation: BlockedUsersOperationName) {
    val blockerId = createVerifiedUsers(1).first().info.id
    val userIdList = createVerifiedUsers(10).map { it.info.id }
    userIdList.forEach { BlockedUsers.create(blockerId, it) }
    val index = 5
    val first = 3
    val expected = userIdList.subList(index + 1, index + 1 + first)
    val actual = when (operation) {
        BlockedUsersOperationName.READ_BLOCKED_USERS -> {
            val cursor = BlockedUsers.read(blockerId).edges[index].cursor
            readBlockedUsers(blockerId, ForwardPagination(first, cursor))
        }
        BlockedUsersOperationName.SEARCH_BLOCKED_USERS -> {
            val cursor = BlockedUsers.search(blockerId, query = "").edges[index].cursor
            searchBlockedUsers(blockerId, query = "", ForwardPagination(first, cursor))
        }
    }
    assertEquals(expected, actual.edges.map { it.node.id })
}

/** The name of a GraphQL operation which is concerned with the pagination of messages. */
private enum class MessagesOperationName {
    /** Represents `Query.searchChatMessages`. */
    SEARCH_CHAT_MESSAGES,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS,
}

/** Asserts that the [operation] paginates correctly. */
private fun testMessagesPagination(operation: MessagesOperationName) {
    val adminId = createVerifiedUsers(1).first().info.id
    val chatId = GroupChats.create(listOf(adminId))
    val text = MessageText("t")
    val messageIdList = (1..10).map { Messages.message(adminId, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    val messages = when (operation) {
        MessagesOperationName.SEARCH_CHAT_MESSAGES -> {
            val messages = searchChatMessages(chatId, text.value, pagination, adminId) as MessageEdges
            messages.edges
        }
        MessagesOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text.value, chatMessagesPagination = pagination).flatMap { it.messages }
        MessagesOperationName.READ_CHATS ->
            readChats(adminId, groupChatMessagesPagination = pagination)[0].messages.edges
        MessagesOperationName.READ_CHAT -> {
            val chat = readChat(chatId, groupChatMessagesPagination = pagination, userId = adminId) as GroupChat
            chat.messages.edges
        }
        MessagesOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, groupChatMessagesPagination = pagination)[0].messages.edges
        }
    }
    assertEquals(messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last), messages.map { it.cursor })
}

/** The name of a GraphQL operation which is concerned with the pagination of contacts. */
private enum class ContactsOperationName {
    /** Represents `Query.readContacts`. */
    READ_CONTACTS,

    /** Represents `Query.searchContacts`. */
    SEARCH_CONTACTS,
}

private fun testContactsPagination(operation: ContactsOperationName) {
    val ownerId = createVerifiedUsers(1).first().info.id
    val userList = createVerifiedUsers(10).map { it.info }
    Contacts.createAll(ownerId, userList.map { it.id }.toSet())
    val index = 5
    val cursor = Contacts.read(ownerId).edges[index].cursor
    val first = 3
    val contacts = when (operation) {
        ContactsOperationName.READ_CONTACTS -> readContacts(ownerId, ForwardPagination(first, cursor))
        ContactsOperationName.SEARCH_CONTACTS ->
            searchContacts(ownerId, query = "username", ForwardPagination(first, cursor))
    }.edges.map { it.node }
    val expected = userList.subList(index + 1, index + 1 + first)
    assertEquals(expected, contacts)
}

/** The name of a GraphQL operation which is concerned with the pagination of accounts. */
private enum class GroupChatUsersOperationName {
    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,
}

private fun testGroupChatUsersPagination(operationName: GroupChatUsersOperationName) {
    val adminId = createVerifiedUsers(1).first().info.id
    val users = createVerifiedUsers(10)
    val userIdList = users.map { it.info.id }
    val chatId = GroupChats.create(listOf(adminId), userIdList)
    val text = "text"
    Messages.create(adminId, chatId, MessageText(text))
    val first = 3
    val userCursors = GroupChatUsers.read()
    val index = 5
    val pagination = ForwardPagination(first, after = userCursors.elementAt(index))
    val chat = when (operationName) {
        GroupChatUsersOperationName.READ_CHAT ->
            readChat(chatId, usersPagination = pagination, userId = adminId) as GroupChat
        GroupChatUsersOperationName.READ_CHATS -> readChats(adminId, usersPagination = pagination)[0]
        GroupChatUsersOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, usersPagination = pagination)[0]
        }
        GroupChatUsersOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text, usersPagination = pagination)[0].chat
    } as GroupChat
    val expected = userCursors.toList().subList(index + 1, index + 1 + first).map { it }
    assertEquals(expected, chat.users.edges.map { it.cursor })
}
