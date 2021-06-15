package com.neelkamath.omniChatBackend.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.Audio
import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*
import java.util.UUID.randomUUID
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class QueriesTest {
    @Nested
    inner class ReadOnlineStatus {
        private fun executeReadOnlineStatus(userId: Int): String {
            val data = executeGraphQlViaEngine(
                """
                query ReadOnlineStatus(${"$"}userId: Int!) {
                    readOnlineStatus(userId: ${"$"}userId) {
                        __typename
                    }
                } 
                """,
                mapOf("userId" to userId),
            ).data!!["readOnlineStatus"] as Map<*, *>
            return data["__typename"] as String
        }

        @Test
        fun `The user must be said to not exist when reading a non-existing user's online status`(): Unit =
            assertEquals("InvalidUserId", executeReadOnlineStatus(userId = -1))

        @Test
        fun `The user's online status must be read`() {
            val userId = createVerifiedUsers(1).first().userId
            assertEquals("OnlineStatus", executeReadOnlineStatus(userId))
        }
    }

    private data class ReadTypingUsersResponse(val chatId: Int, val users: List<Account>) {
        data class Account(val userId: Int)
    }

    @Nested
    inner class ReadTypingUsers {
        @Test
        fun `Only users (including the user themselves) who are typing must have their typing statuses read`() {
            val (admin1Id, admin2Id, admin3Id) = createVerifiedUsers(3).map { it.userId }
            val (chat1Id, chat2Id, chat3Id) = (1..3).map { GroupChats.create(setOf(admin1Id, admin2Id, admin3Id)) }
            TypingStatuses.update(chat1Id, admin1Id, isTyping = true)
            TypingStatuses.update(chat2Id, admin2Id, isTyping = true)
            val data = executeGraphQlViaEngine(
                """
                query ReadTypingUsers {
                    readTypingUsers {
                        chatId
                        users {
                            userId
                        }
                    }
                } 
                """,
                userId = admin1Id,
            ).data!!["readTypingUsers"] as List<*>
            val expected = listOf(
                ReadTypingUsersResponse(chat1Id, listOf(ReadTypingUsersResponse.Account(admin1Id))),
                ReadTypingUsersResponse(chat2Id, listOf(ReadTypingUsersResponse.Account(admin2Id))),
                ReadTypingUsersResponse(chat3Id, listOf()),
            )
            val actual = testingObjectMapper.convertValue<List<ReadTypingUsersResponse>>(data)
            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class ReadAccount {
        @Test
        fun `The specified user's account must be read`() {
            val userId = createVerifiedUsers(1).first().userId
            val data = executeGraphQlViaEngine(
                """
                query ReadAccount(${"$"}userId: Int!) {
                    readAccount(userId: ${"$"}userId) {
                        __typename
                    }
                }
                """,
                mapOf("userId" to userId),
            ).data!!["readAccount"] as Map<*, *>
            assertEquals("Account", data["__typename"])
        }
    }

    private data class ReadChatResponse(val __typename: String, val messages: Messages?) {
        data class Messages(val edges: List<Edge>) {
            data class Edge(val node: Node) {
                data class Node(val hasStar: Boolean)
            }
        }
    }

    @Nested
    inner class ReadChat {
        private val readChatQuery = """
            query ReadChat(${"$"}id: Int!) {
                readChat(id: ${"$"}id) {
                    __typename
                    ... on GroupChat {
                        messages {
                            edges {
                                node {
                                    hasStar
                                }
                            }
                        }
                    }
                }
            }
        """

        @Test
        fun `Reading a public chat mustn't require an access token`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val data = executeGraphQlViaEngine(readChatQuery, mapOf("id" to chatId)).data!!["readChat"] as Map<*, *>
            assertEquals("GroupChat", data["__typename"])
        }

        @Test
        fun `Reading a public chat with an access token must return the chat as it's viewed by the user`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val data =
                executeGraphQlViaEngine(readChatQuery, mapOf("id" to chatId), adminId).data!!["readChat"] as Map<*, *>
            testingObjectMapper.convertValue<ReadChatResponse>(data).messages!!.edges[0].node.hasStar.let(::assertTrue)
        }

        @Test
        fun `Reading a public chat the user isn't in using an access token must work`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val data =
                executeGraphQlViaEngine(readChatQuery, mapOf("id" to chatId), userId).data!!["readChat"] as Map<*, *>
            assertEquals("GroupChat", testingObjectMapper.convertValue<ReadChatResponse>(data).__typename)
        }

        @Test
        fun `The chat must be said to not exist when a non-existing chat is trying to be read`() {
            val adminId = createVerifiedUsers(1).first().userId
            val data =
                executeGraphQlViaEngine(readChatQuery, mapOf("id" to -1), adminId).data!!["readChat"] as Map<*, *>
            assertEquals("InvalidChatId", data["__typename"])
        }

        @Test
        fun `Reading a non-public chat must require authorization`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val status = executeGraphQlViaHttp(readChatQuery, mapOf("id" to chatId)).status()
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        @Test
        fun `The user's chat must be read`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val data =
                executeGraphQlViaEngine(readChatQuery, mapOf("id" to chatId), adminId).data!!["readChat"] as Map<*, *>
            assertEquals("GroupChat", data["__typename"])
        }
    }

    /** The [adminId] of the group [chatIdList] which are sorted in ascending order. */
    private data class CreatedChats(val adminId: Int, val chatIdList: LinkedHashSet<Int>)

    private data class ReadChatsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val chatId: Int)
        }
    }

    @Nested
    inner class ReadChats {
        /** Creates the [count] of [CreatedChats.chatIdList]. */
        private fun createChats(count: Int = 10): CreatedChats {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..count).map { GroupChats.create(setOf(adminId)) }.toLinkedHashSet()
            return CreatedChats(adminId, chatIdList)
        }

        private fun executeReadChats(adminId: Int, pagination: ForwardPagination? = null): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query ReadChats(${"$"}first: Int, ${"$"}after: Cursor) {
                    readChats(first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                chatId
                            }
                        }
                    }
                }
                """,
                mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
                adminId,
            ).data!!["readChats"] as Map<*, *>
            return testingObjectMapper.convertValue<ReadChatsResponse>(data).edges.map { it.node.chatId }
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (admin, chatIdList) = createChats()
            assertEquals(chatIdList, executeReadChats(admin).toLinkedHashSet())
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, chatIdList) = createChats()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = executeReadChats(adminId, pagination).toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, chatIdList) = createChats()
            val first = 3
            val actual = executeReadChats(adminId, ForwardPagination(first))
            assertEquals(chatIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, chatIdList) = createChats()
            val index = 4
            val pagination = ForwardPagination(after = chatIdList.elementAt(index))
            assertEquals(chatIdList.drop(index + 1), executeReadChats(adminId, pagination))
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (adminId, chatIdList) = createChats()
            val pagination = ForwardPagination(after = chatIdList.last())
            assertEquals(0, executeReadChats(adminId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`(): Unit =
            runBlocking {
                val (adminId, chatIdList) = createChats()
                GroupChatUsers.removeUsers(chatIdList.elementAt(3), adminId)
                val pagination = ForwardPagination(first = 3, after = chatIdList.elementAt(1))
                assertEquals(listOf(2, 4, 5).map(chatIdList::elementAt), executeReadChats(adminId, pagination))
            }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`(): Unit =
            runBlocking {
                val (adminId, chatIdList) = createChats()
                val index = 4
                val chatId = chatIdList.elementAt(index)
                GroupChatUsers.removeUsers(chatId, adminId)
                val actual = executeReadChats(adminId, ForwardPagination(after = chatId))
                assertEquals(chatIdList.drop(index + 1), actual)
            }
    }

    private data class ReadContactsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val userId: Int)
        }
    }

    @Nested
    inner class ReadContacts {
        @Test
        fun `The user's contacts must be read as per the pagination`() {
            val contactOwnerId = createVerifiedUsers(1).first().userId
            val contactUserIdList = createVerifiedUsers(10).map { it.userId }
            contactUserIdList.forEach { Contacts.create(contactOwnerId, it) }
            val first = 3
            val index = 4
            val data = executeGraphQlViaEngine(
                """
                query ReadContacts(${"$"}first: Int, ${"$"}after: Cursor) {
                    readContacts(first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                userId
                            }
                        }
                    }
                }
                """,
                mapOf("first" to first, "after" to contactUserIdList.elementAt(index).toString()),
                contactOwnerId,
            ).data!!["readContacts"] as Map<*, *>
            val actual = testingObjectMapper.convertValue<ReadContactsResponse>(data).edges.map { it.node.userId }
            assertEquals(contactUserIdList.slice(index + 1..index + first), actual)
        }
    }

    @Nested
    inner class RequestTokenSet {
        private fun executeRequestTokenSet(login: Login): Map<*, *> = executeGraphQlViaEngine(
            """
            query RequestTokenSet(${"$"}login: Login!) {
                requestTokenSet(login: ${"$"}login) {
                    __typename
                    ... on TokenSet {
                        accessToken
                        refreshToken
                    }
                }
            }
            """,
            mapOf("login" to login),
        ).data!!["requestTokenSet"] as Map<*, *>

        @Test
        fun `The access and refresh tokens must expire in one hour, and one week respectively`() {
            val login = createVerifiedUsers(1).first().login
            val data = executeRequestTokenSet(login)
            val accessTokenExpiry = jwtVerifier.verify(data["accessToken"] as String).expiry!!
            val inOneHour = LocalDateTime.now().plusHours(1)
            assertTrue(accessTokenExpiry > inOneHour.minusMinutes(1))
            assertTrue(accessTokenExpiry < inOneHour.plusMinutes(1))
            val refreshTokenExpiry = jwtVerifier.verify(data["refreshToken"] as String).expiry!!
            val inOneWeek = LocalDateTime.now().plusWeeks(1)
            assertTrue(refreshTokenExpiry > inOneWeek.minusMinutes(1))
            assertTrue(refreshTokenExpiry < inOneWeek.plusMinutes(1))
        }

        @Test
        fun `The user must be said to not exist when requesting a token set for a non-existing user`() {
            val login = Login(Username("u"), Password("p"))
            assertEquals("NonexistingUser", executeRequestTokenSet(login)["__typename"])
        }

        @Test
        fun `The user's email address must be said to be unverified`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            Users.create(account)
            val login = Login(account.username, account.password)
            assertEquals("UnverifiedEmailAddress", executeRequestTokenSet(login)["__typename"])
        }

        @Test
        fun `The password must be said to be incorrect`() {
            val username = createVerifiedUsers(1).first().username
            val login = Login(username, Password("incorrect"))
            assertEquals("IncorrectPassword", executeRequestTokenSet(login)["__typename"])
        }

        @Test
        fun `The access token must work`() {
            val login = createVerifiedUsers(1).first().login
            val token = executeRequestTokenSet(login)["accessToken"] as String
            val expected = mapOf(
                "data" to mapOf(
                    "readAccount" to mapOf("__typename" to "Account"),
                ),
            )
            val actual = readGraphQlHttpResponse(
                """
                query ReadAccount {
                    readAccount {
                        __typename
                    }
                }
                """,
                mapOf("login" to login),
                token,
            )
            assertEquals(expected, actual)
        }

        @Test
        fun `The refresh token must work`() {
            val login = createVerifiedUsers(1).first().login
            val token = executeRequestTokenSet(login)["refreshToken"]
            val expected = mapOf(
                "data" to mapOf(
                    "refreshTokenSet" to mapOf("__typename" to "TokenSet"),
                ),
            )
            val actual = readGraphQlHttpResponse(
                """
                query RefreshTokenSet(${"$"}refreshToken: ID!) {
                    refreshTokenSet(refreshToken: ${"$"}refreshToken) {
                        __typename
                    }
                }
                """,
                mapOf("refreshToken" to token),
            )
            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class RefreshTokenSet {
        @Test
        fun `The token set must get refreshed`() {
            val userId = createVerifiedUsers(1).first().userId
            val token = buildTokenSet(userId).refreshToken.value
            val data = executeGraphQlViaEngine(
                """
                query RefreshTokenSet(${"$"}refreshToken: ID!) {
                    refreshTokenSet(refreshToken: ${"$"}refreshToken) {
                        __typename
                    }
                }
                """,
                mapOf("refreshToken" to token),
            ).data!!["refreshTokenSet"] as Map<*, *>
            assertEquals("TokenSet", data["__typename"])
        }
    }

    private data class SearchChatMessagesResponse(val __typename: String, val edges: List<Edge>?) {
        data class Edge(val node: Node) {
            data class Node(val messageId: Int, val hasStar: Boolean)
        }
    }

    @Nested
    inner class SearchChatMessages {
        private fun executeSearchChatMessages(
            userId: Int,
            chatId: Int,
            query: String = "",
            pagination: BackwardPagination? = null,
        ): SearchChatMessagesResponse {
            val data = executeGraphQlViaEngine(
                """
                query SearchChatMessages(
                    ${"$"}chatId: Int!
                    ${"$"}query: String!
                    ${"$"}last: Int
                    ${"$"}before: Cursor
                ) {
                    searchChatMessages(
                        chatId: ${"$"}chatId
                        query: ${"$"}query
                        last: ${"$"}last
                        before: ${"$"}before
                    ) {
                        __typename
                        ... on MessageEdges {
                            edges {
                                node {
                                    messageId
                                    hasStar
                                }
                            }
                        }
                    }
                }
                """,
                mapOf(
                    "chatId" to chatId,
                    "query" to query.uppercase(),
                    "last" to pagination?.last,
                    "before" to pagination?.before?.toString(),
                ),
                userId,
            ).data!!["searchChatMessages"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `The chat must be searched case-insensitively as per the pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val query = "matched"
            val messageIdList = (1..10).map {
                Messages.message(adminId, chatId, MessageText("not matching"))
                Messages.message(adminId, chatId, MessageText(query))
            }
            val last = 3
            val index = 7
            val pagination = BackwardPagination(last, before = messageIdList[index])
            val actual = executeSearchChatMessages(adminId, chatId, query, pagination).edges!!.map { it.node.messageId }
            assertEquals(messageIdList.subList(index - last, index), actual)
        }

        @Test
        fun `The chat must be said not exist`() {
            val userId = createVerifiedUsers(1).first().userId
            val data = executeSearchChatMessages(userId, chatId = -1)
            assertEquals("InvalidChatId", data.__typename)
        }

        @Test
        fun `Searching a public chat with an access token must return the chat as it's viewed by the user`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            executeSearchChatMessages(adminId, chatId).edges!![0].node.hasStar.let(::assertTrue)
        }
    }

    private data class SearchChatsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val chatId: Int)
        }
    }

    @Nested
    inner class SearchChats {
        private fun executeSearchChats(userId: Int, pagination: ForwardPagination? = null): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query SearchChats(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    searchChats(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                chatId
                            }
                        }
                    }
                }
                """,
                mapOf("query" to "", "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["searchChats"] as Map<*, *>
            return testingObjectMapper.convertValue<SearchChatsResponse>(data).edges.map { it.node.chatId }
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
            assertEquals(chatIdList, executeSearchChats(adminId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = chatIdList[index])
            assertEquals(chatIdList.slice(index + 1..index + first), executeSearchChats(adminId, pagination))
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
            val first = 3
            val actual = executeSearchChats(adminId, ForwardPagination(first))
            assertEquals(chatIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
            val index = 4
            val actual = executeSearchChats(adminId, ForwardPagination(after = chatIdList[index]))
            assertEquals(chatIdList.drop(index + 1), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
            val pagination = ForwardPagination(after = chatIdList.last())
            assertEquals(0, executeSearchChats(adminId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
                GroupChatUsers.removeUsers(chatIdList[3], adminId)
                val actual = executeSearchChats(adminId, ForwardPagination(first = 3, after = chatIdList[1]))
                assertEquals(listOf(chatIdList[2], chatIdList[4], chatIdList[5]), actual)
            }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatIdList = (1..10).map { GroupChats.create(setOf(adminId)) }
                val index = 4
                GroupChatUsers.removeUsers(chatIdList[index], adminId)
                val actual = executeSearchChats(adminId, ForwardPagination(after = chatIdList[index]))
                assertEquals(chatIdList.drop(index + 1), actual)
            }

        @Test
        fun `Private and group chats must get paginated together`() {
            val (user1Id, user2Id, user3Id, user4Id, user5Id, user6Id) = createVerifiedUsers(6).map { it.userId }
            val groupChatCreator = { GroupChats.create(adminIdList = listOf(user1Id)) }
            val chatIdList = listOf(
                groupChatCreator(),
                PrivateChats.create(user1Id, user2Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user3Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user4Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user5Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user6Id),
                groupChatCreator(),
            )
            val first = 3
            val index = 4
            val actual = executeSearchChats(user1Id, ForwardPagination(first, after = chatIdList[index]))
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }
    }

    private data class ReadStarsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val messageId: Int)
        }
    }

    @Nested
    inner class ReadStars {
        @Test
        fun `Messages must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map {
                Messages.message(adminId, chatId).also { Stargazers.create(adminId, it) }
            }
            val first = 3
            val index = 4
            val data = executeGraphQlViaEngine(
                """
                query ReadStars(${"$"}first: Int, ${"$"}after: Cursor) {
                    readStars(first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                messageId
                            }
                        }
                    }
                }
                """,
                mapOf("first" to first, "after" to messageIdList[index].toString()),
                adminId,
            ).data!!["readStars"] as Map<*, *>
            val actual = testingObjectMapper.convertValue<ReadStarsResponse>(data).edges.map { it.node.messageId }
            assertEquals(messageIdList.slice(index + 1..index + first), actual)
        }
    }

    private data class SearchContactsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val userId: Int)
        }
    }

    @Nested
    inner class SearchContacts {
        private fun executeSearchContacts(
            userId: Int,
            query: String = "",
            pagination: ForwardPagination? = null,
        ): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query SearchContacts(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    searchContacts(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                userId
                            }
                        }
                    }
                }
                """,
                mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["searchContacts"] as Map<*, *>
            return testingObjectMapper.convertValue<SearchContactsResponse>(data).edges.map { it.node.userId }
        }

        @Test
        fun `Contacts must be searched case-insensitively by their usernames, first names, last names, and email addresses`() {
            val contactOwnerUserId = createVerifiedUsers(1).first().userId
            val query = "mango"
            val contactUserIdList = listOf(
                AccountInput(Username(query), Password("p"), "1@example.com"),
                AccountInput(Username("2"), Password("p"), "2@example.com", firstName = Name(query)),
                AccountInput(Username("3"), Password("p"), "3@example.com", lastName = Name(query)),
                AccountInput(Username("4"), Password("p"), "$query@example.com"),
                AccountInput(Username("5"), Password("p"), "5@example.com"),
            ).map {
                Users.create(it)
                val userId = Users.readId(it.username)
                Contacts.create(contactOwnerUserId, userId)
                verifyEmailAddress(it.username)
                userId
            }
            assertEquals(contactUserIdList.dropLast(1), executeSearchContacts(contactOwnerUserId, query))
        }

        @Test
        fun `Contacts must be paginated`() {
            val contactOwnerUserId = createVerifiedUsers(1).first().userId
            val contactUserIdList = createVerifiedUsers(10).map { (contactUserId) ->
                Contacts.create(contactOwnerUserId, contactUserId)
                contactUserId
            }
            val first = 3
            val index = 4
            val expected = contactUserIdList.slice(index + 1..index + first)
            val pagination = ForwardPagination(first, after = contactUserIdList[index])
            assertEquals(expected, executeSearchContacts(contactOwnerUserId, pagination = pagination))
        }
    }

    private data class SearchMessagesResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val chat: Chat, val messages: List<Message>) {
                data class Chat(val chatId: Int)
                data class Message(val node: Node) {
                    data class Node(val messageId: Int)
                }
            }
        }
    }

    @Nested
    inner class SearchMessages {
        private fun executeSearchMessages(
            userId: Int,
            query: String = "",
            pagination: ForwardPagination? = null,
        ): SearchMessagesResponse {
            val data = executeGraphQlViaEngine(
                """
                query SearchMessages(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    searchMessages(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                chat {
                                    chatId
                                }
                                messages {
                                    node {
                                        messageId
                                    }
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["searchMessages"]!!
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `Searchable messages must be found`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val query = "Kotlin"
            val expected = listOf(query, "C++")
                .map {
                    listOf(
                        // Testing text messages.
                        Messages.message(adminId, chatId, MessageText(it)),
                        // Testing poll message titles.
                        Messages.message(
                            adminId,
                            chatId,
                            PollInput(title = MessageText(it), listOf(MessageText("Yes"), MessageText("No"))),
                        ),
                        // Testing poll message options.
                        Messages.message(
                            adminId,
                            chatId,
                            PollInput(MessageText("Title"), listOf(MessageText(it), MessageText("No"))),
                        ),
                        // Testing action message texts.
                        Messages.message(
                            adminId,
                            chatId,
                            ActionMessageInput(MessageText(it), listOf(MessageText("Yes"), MessageText("No"))),
                        ),
                        // Testing action message actions.
                        Messages.message(
                            adminId,
                            chatId,
                            ActionMessageInput(MessageText("Text"), listOf(MessageText(it), MessageText("No"))),
                        ),
                        // Testing pic message captions.
                        Messages.message(
                            adminId,
                            chatId,
                            CaptionedPic(readPic("76px×57px.jpg"), caption = MessageText(it)),
                        ),
                        // Testing messages which cannot be searched.
                        Messages.message(adminId, chatId, Audio(ByteArray(1))),
                    )
                }[0]
                .dropLast(1)
            val actual = executeSearchMessages(adminId, query).edges.flatMap { (node) ->
                node.messages.map { it.node.messageId }
            }
            assertEquals(expected, actual)
        }

        @Test
        fun `Private and group chats must get paginated together`() {
            val (user1Id, user2Id, user3Id, user4Id, user5Id, user6Id) = createVerifiedUsers(6).map { it.userId }
            val groupChatCreator = { GroupChats.create(adminIdList = listOf(user1Id)) }
            val chatIdList = listOf(
                groupChatCreator(),
                PrivateChats.create(user1Id, user2Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user3Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user4Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user5Id),
                groupChatCreator(),
                PrivateChats.create(user1Id, user6Id),
            ).map {
                Messages.message(user1Id, it)
                it
            }
            val first = 3
            val index = 4
            val paginatedChatIdList =
                executeSearchMessages(user1Id, pagination = ForwardPagination(first, after = chatIdList[index]))
                    .edges
                    .map { it.node.chat.chatId }
            assertEquals(chatIdList.slice(index + 1..index + first), paginatedChatIdList)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "matched"
            val chatIdList = (1..10).map {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
            }
            val paginatedChatIdList = executeSearchMessages(adminId, query).edges.map { it.node.chat.chatId }
            assertEquals(chatIdList, paginatedChatIdList)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "matched"
            val chatIdList = (1..10).map {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
            }
            val first = 3
            val index = 4
            val paginatedChatIdList =
                executeSearchMessages(adminId, query, ForwardPagination(first, after = chatIdList[index]))
                    .edges
                    .map { it.node.chat.chatId }
            assertEquals(chatIdList.slice(index + 1..index + first), paginatedChatIdList)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "matched"
            val chatIdList = (1..10).map {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
            }
            val first = 3
            val paginatedChatIdList =
                executeSearchMessages(adminId, query, ForwardPagination(first)).edges.map { it.node.chat.chatId }
            assertEquals(chatIdList.take(first), paginatedChatIdList)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "matched"
            val chatIdList = (1..10).map {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
            }
            val index = 4
            val paginatedChatIdList =
                executeSearchMessages(adminId, query, ForwardPagination(after = chatIdList[index]))
                    .edges
                    .map { it.node.chat.chatId }
            assertEquals(chatIdList.drop(index + 1), paginatedChatIdList)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "matched"
            val chatIdList = (1..10).map {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
            }
            val pagination = ForwardPagination(after = chatIdList.last())
            val paginatedChatIdList =
                executeSearchMessages(adminId, query, pagination).edges.map { it.node.chat.chatId }
            assertEquals(0, paginatedChatIdList.size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val query = "matched"
                val chatIdList = (1..10).map {
                    GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
                }
                GroupChatUsers.removeUsers(chatIdList[3], adminId)
                val actual = executeSearchMessages(adminId, query, ForwardPagination(first = 3, after = chatIdList[1]))
                    .edges
                    .map { it.node.chat.chatId }
                assertEquals(listOf(chatIdList[2], chatIdList[4], chatIdList[5]), actual)
            }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val query = "matched"
                val chatIdList = (1..10).map {
                    GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it, MessageText(query)) }
                }
                val index = 4
                GroupChatUsers.removeUsers(adminId, chatIdList[index])
                val paginatedChatIdList =
                    executeSearchMessages(adminId, query, ForwardPagination(after = chatIdList[index]))
                        .edges
                        .map { it.node.chat.chatId }
                assertEquals(chatIdList.drop(index + 1), paginatedChatIdList)
            }
    }

    private data class SearchUsersResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val userId: Int)
        }
    }

    private fun executeSearchUsers(query: String = "", pagination: ForwardPagination? = null): List<Int> {
        val data = executeGraphQlViaEngine(
            """
            query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                    edges {
                        node {
                            userId
                        }
                    }
                }
            }
            """,
            mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
        ).data!!["searchUsers"] as Map<*, *>
        return testingObjectMapper.convertValue<SearchUsersResponse>(data).edges.map { it.node.userId }
    }

    @Nested
    inner class SearchUsers {
        @Test
        fun `Users must be searched case-insensitively by their usernames, first names, last names, and email addresses`() {
            val query = "mango"
            val expected = listOf(
                AccountInput(Username(query), Password("p"), "1@example.com"),
                AccountInput(Username("4"), Password("p"), "$query@example.com"),
                AccountInput(Username("2"), Password("p"), "2@example.com", firstName = Name(query)),
                AccountInput(Username("3"), Password("p"), "3@example.com", lastName = Name(query)),
                AccountInput(Username("5"), Password("p"), "5@example.com"),
            )
                .map {
                    Users.create(it)
                    Users.readId(it.username)
                }
                .dropLast(1)
            assertEquals(expected, executeSearchUsers(query))
        }

        @Test
        fun `Users must be paginated`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val first = 3
            val index = 4
            val actual = executeSearchUsers(pagination = ForwardPagination(first, after = userIdList[index]))
            assertEquals(userIdList.slice(index + 1..index + first), actual)
        }
    }

    private data class ReadMessageResponse(
        val statusCode: HttpStatusCode,
        val result: ReadMessageResult.Data.ReadMessage?,
    )

    private data class ReadMessageResult(val data: Data) {
        data class Data(val readMessage: ReadMessage) {
            data class ReadMessage(val __typename: String, val hasStar: Boolean)
        }
    }

    @Nested
    inner class ReadMessage {
        private fun executeReadMessage(accessToken: String? = null, messageId: Int): ReadMessageResponse {
            val response = executeGraphQlViaHttp(
                """
                query ReadMessage(${"$"}messageId: Int!) {
                    readMessage(messageId: ${"$"}messageId) {
                        __typename
                        hasStar
                    }
                }
                """,
                mapOf("messageId" to messageId),
                accessToken,
            )
            val result = response.content?.let { testingObjectMapper.readValue<ReadMessageResult>(it).data.readMessage }
            return ReadMessageResponse(response.status()!!, result)
        }

        @Test
        fun `The public chat message must be read as viewed by the user and anonymous user`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(admin.userId, chatId)
            Stargazers.create(admin.userId, messageId)
            assertEquals(
                ReadMessageResult.Data.ReadMessage("TextMessage", hasStar = true),
                executeReadMessage(admin.accessToken, messageId).result,
            )
            assertEquals(
                ReadMessageResult.Data.ReadMessage("TextMessage", hasStar = false),
                executeReadMessage(messageId = messageId).result,
            )
        }

        @Test
        fun `Attempting to read a message the user can't see must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals(HttpStatusCode.Unauthorized, executeReadMessage(messageId = messageId).statusCode)
        }

        @Test
        fun `Attempting to read a non-existing message must fail`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.Unauthorized, executeReadMessage(token, messageId = -1).statusCode)
        }
    }

    @Nested
    inner class PaginateUserIdList {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            assertEquals(userIdList, executeSearchUsers())
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val first = 3
            val index = 4
            val actual = executeSearchUsers(pagination = ForwardPagination(first, after = userIdList[index]))
            assertEquals(userIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val first = 3
            val actual = executeSearchUsers(pagination = ForwardPagination(first))
            assertEquals(userIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val index = 4
            val actual = executeSearchUsers(pagination = ForwardPagination(after = userIdList[index]))
            assertEquals(userIdList.drop(index + 1), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val pagination = ForwardPagination(after = userIdList.last())
            assertEquals(0, executeSearchUsers(pagination = pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`(): Unit =
            runBlocking {
                val userIdList = createVerifiedUsers(10).map { it.userId }
                deleteUser(userIdList[3])
                val actual = executeSearchUsers(pagination = ForwardPagination(first = 3, after = userIdList[1]))
                assertEquals(listOf(userIdList[2], userIdList[4], userIdList[5]), actual)
            }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`(): Unit =
            runBlocking {
                val userIdList = createVerifiedUsers(10).map { it.userId }
                val index = 4
                deleteUser(userIdList[index])
                val actual = executeSearchUsers(pagination = ForwardPagination(after = userIdList[index]))
                assertEquals(userIdList.drop(index + 1), actual)
            }
    }

    private data class SearchBlockedUsersResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val userId: Int)
        }
    }

    @Nested
    inner class SearchBlockedUsers {
        private fun executeSearchBlockedUsers(
            userId: Int,
            query: String = "",
            pagination: ForwardPagination? = null,
        ): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query SearchBlockedUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    searchBlockedUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                userId
                            }
                        }
                    }
                }
                """,
                mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["searchBlockedUsers"] as Map<*, *>
            return testingObjectMapper.convertValue<SearchBlockedUsersResponse>(data).edges.map { it.node.userId }
        }

        @Test
        fun `Users must be searched case-insensitively by their usernames, first names, last names, and email addresses`() {
            val blockerUserId = createVerifiedUsers(1).first().userId
            val query = "matching"
            val expected = listOf(
                AccountInput(Username(query), Password("p"), "1@example.com"),
                AccountInput(Username("2"), Password("p"), "$query@example.com"),
                AccountInput(Username("3"), Password("p"), "3@example.com", firstName = Name(query)),
                AccountInput(Username("4"), Password("p"), "4@example.com", lastName = Name(query)),
                AccountInput(Username("5"), Password("p"), "5@example.com"),
            )
                .map {
                    Users.create(it)
                    val blockedUserId = Users.readId(it.username)
                    BlockedUsers.create(blockerUserId, blockedUserId)
                    blockedUserId
                }
                .dropLast(1)
            assertEquals(expected, executeSearchBlockedUsers(blockerUserId, query))
        }

        @Test
        fun `Users must be paginated`() {
            val blockerUserId = createVerifiedUsers(1).first().userId
            val blockedUserIdList = createVerifiedUsers(10).map { (blockedUserId) ->
                BlockedUsers.create(blockerUserId, blockedUserId)
                blockedUserId
            }
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = blockedUserIdList[index])
            val actual = executeSearchBlockedUsers(blockerUserId, pagination = pagination)
            assertEquals(blockedUserIdList.slice(index + 1..index + first), actual)
        }
    }

    private data class ReadBlockedUsersResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val userId: Int)
        }
    }

    @Nested
    inner class ReadBlockedUsers {
        private fun executeReadBlockedUsers(userId: Int, pagination: ForwardPagination? = null): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query ReadBlockedUsers(${"$"}first: Int, ${"$"}after: Cursor) {
                    readBlockedUsers(first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                userId
                            }
                        }
                    }
                }
                """,
                mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["readBlockedUsers"] as Map<*, *>
            return testingObjectMapper.convertValue<ReadBlockedUsersResponse>(data).edges.map { it.node.userId }
        }

        @Test
        fun `Blocked users must be paginated`() {
            val blockerUserId = createVerifiedUsers(1).first().userId
            val blockedUserIdList = createVerifiedUsers(10).map { (blockedUserId) ->
                BlockedUsers.create(blockerUserId, blockedUserId)
                blockedUserId
            }
            val first = 3
            val index = 4
            val actual =
                executeReadBlockedUsers(blockerUserId, ForwardPagination(first, after = blockedUserIdList[index]))
            assertEquals(blockedUserIdList.slice(index + 1..index + first), actual)
        }
    }

    @Nested
    inner class ReadGroupChat {
        private fun executeReadGroupChat(inviteCode: UUID): String {
            val data = executeGraphQlViaEngine(
                """
                query ReadGroupChat(${"$"}inviteCode: Uuid!) {
                    readGroupChat(inviteCode: ${"$"}inviteCode) {
                        __typename
                    }
                }
                """,
                mapOf("inviteCode" to inviteCode.toString()),
            ).data!!["readGroupChat"] as Map<*, *>
            return data["__typename"] as String
        }

        @Test
        fun `Using an invalid invite code must state as such`() {
            val actual = executeReadGroupChat(inviteCode = randomUUID())
            assertEquals("InvalidInviteCode", actual)
        }

        @Test
        fun `Using an invite code from a non-invitable must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val inviteCode = GroupChats.readInviteCode(chatId)!!
            GroupChats.setInvitability(chatId, isInvitable = false)
            assertEquals("InvalidInviteCode", executeReadGroupChat(inviteCode))
        }

        @Test
        fun `The chat must be read`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val inviteCode = GroupChats.readInviteCode(chatId)!!
            assertEquals("GroupChatInfo", executeReadGroupChat(inviteCode))
        }
    }

    private data class SearchPublicChatsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val chatId: Int, val messages: Messages) {
                data class Messages(val edges: List<Edge>) {
                    data class Edge(val node: Node) {
                        data class Node(val hasStar: Boolean)
                    }
                }
            }
        }
    }

    @Nested
    inner class SearchPublicChats {
        private fun executeSearchPublicChats(
            userId: Int,
            query: String = "",
            pagination: ForwardPagination? = null,
        ): SearchPublicChatsResponse {
            val data = executeGraphQlViaEngine(
                """
                query SearchPublicChats(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    searchPublicChats(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            node {
                                chatId
                                messages {
                                    edges {
                                        node {
                                            hasStar
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["searchPublicChats"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `Chats must be case-insensitively queried`() {
            val adminId = createVerifiedUsers(1).first().userId
            val query = "Kotlin"
            val chatIdList = (1..5).map {
                GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
                GroupChats.create(setOf(adminId), title = GroupChatTitle(query), publicity = GroupChatPublicity.PUBLIC)
            }
            assertEquals(chatIdList, executeSearchPublicChats(adminId, query).edges.map { it.node.chatId })
        }

        @Test
        fun `Chats must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = (1..10).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC) }
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = chatIdList[index])
            val actual = executeSearchPublicChats(adminId, pagination = pagination).edges.map { it.node.chatId }
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `Searching chats with an access token must return chats as viewed by the user`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val hasStar = executeSearchPublicChats(adminId).edges[0].node.messages.edges.first().node.hasStar
            assertTrue(hasStar)
        }
    }
}
