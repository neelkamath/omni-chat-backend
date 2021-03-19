package com.neelkamath.omniChat.graphql.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.graphql.operations.READ_ACCOUNT_QUERY
import com.neelkamath.omniChat.graphql.operations.STARRED_MESSAGE_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.TYPING_USERS_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATE_GROUP_CHAT_TITLE_QUERY
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class QueriesAndMutationsTest {
    @Nested
    inner class RouteGraphQlQueriesAndMutations {
        @Test
        fun `The GraphQL engine must be queried via the HTTP interface`() {
            val user = createVerifiedUsers(1).first()
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = user.accessToken).content!!
            val data = testingObjectMapper.readValue<GraphQlResponse>(response).data!!["readAccount"]!!
            assertEquals(user.info, testingObjectMapper.convertValue(data))
        }

        private fun testOperationName(mustSupplyOperationName: Boolean) {
            val accessToken = createVerifiedUsers(1).first().accessToken
            val call = withTestApplication(Application::main) {
                handleRequest(HttpMethod.Post, "query-or-mutation") {
                    val query = """
                        query ReadStars {
                            readStars {
                                $STARRED_MESSAGE_FRAGMENT
                            }
                        }
                        
                        query ReadTypingUsers {
                            readTypingUsers {
                                $TYPING_USERS_FRAGMENT
                            }
                        }
                    """
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
                    val operationName = "ReadStars".takeIf { mustSupplyOperationName }
                    val body = GraphQlRequest(query, operationName = operationName)
                    setBody(testingObjectMapper.writeValueAsString(body))
                }
            }
            assertEquals(if (mustSupplyOperationName) 200 else 400, call.response.status()!!.value)
        }

        @Test
        fun `The specified operation must be executed when there are multiple`() {
            testOperationName(mustSupplyOperationName = true)
        }

        @Test
        fun `An error must be returned when multiple operations are supplied without an operation name`() {
            testOperationName(mustSupplyOperationName = false)
        }

        @Test
        fun `An HTTP status code of 401 must be received when supplying an invalid access token`() {
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = "invalid token")
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `An HTTP status code of 401 must be received when supplying the token of a user who lacks permissions`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_TITLE_QUERY,
                mapOf("chatId" to chatId, "title" to "T"),
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}
