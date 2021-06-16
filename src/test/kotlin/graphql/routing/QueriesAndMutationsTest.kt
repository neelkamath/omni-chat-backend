package com.neelkamath.omniChatBackend.graphql.routing

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

/** [executeGraphQlViaHttp] wrapper which parses the [TestApplicationResponse.content] as a [Map]. */
fun readGraphQlHttpResponse(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null,
): Map<String, Any> = executeGraphQlViaHttp(query, variables, accessToken).content!!.let(testingObjectMapper::readValue)

/**
 * Executes GraphQL queries and mutations via the HTTP interface. The [variables] are for the [query], which is the
 * GraphQL doc.
 *
 * @see readGraphQlHttpResponse
 * @see executeGraphQlViaEngine
 */
fun executeGraphQlViaHttp(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "query-or-mutation") {
        accessToken?.let { addHeader(HttpHeaders.Authorization, "Bearer $it") }
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val body = GraphQlRequest(query, variables)
        setBody(testingObjectMapper.writeValueAsString(body))
    }.response
}

@ExtendWith(DbExtension::class)
class QueriesAndMutationsTest {
    @Nested
    inner class RouteGraphQlQueriesAndMutations {
        @Test
        fun `The GraphQL engine must be queried via the HTTP interface`() {
            val user = createVerifiedUsers(1).first()
            val response = readGraphQlHttpResponse(
                """
                query ReadStars {
                    readStars {
                        __typename
                    }
                }
                """,
                accessToken = user.accessToken,
            )["data"] as Map<*, *>
            val result = response["readStars"] as Map<*, *>
            assertEquals(user.userId, result["__typename"])
        }

        private fun testOperationName(mustSupplyOperationName: Boolean) {
            val accessToken = createVerifiedUsers(1).first().accessToken
            val call = withTestApplication(Application::main) {
                handleRequest(HttpMethod.Post, "query-or-mutation") {
                    val query = """
                        query ReadStars {
                            readStars {
                                __typename
                            }
                        }
                        
                        query ReadTypingUsers {
                            readTypingUsers {
                                __typename
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
        fun `The specified operation must be executed when there are multiple`(): Unit =
            testOperationName(mustSupplyOperationName = true)

        @Test
        fun `An error must be returned when multiple operations are supplied without an operation name`(): Unit =
            testOperationName(mustSupplyOperationName = false)

        @Test
        fun `An HTTP status code of 401 must be received when supplying an invalid access token`() {
            val response = executeGraphQlViaHttp(
                """
                query ReadAccount {
                    readAccount {
                        __typename
                    }
                }
                """,
                accessToken = "invalid",
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `An HTTP status code of 401 must be received when supplying the token of a user who lacks permissions`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId), listOf(user.userId))
            val response = executeGraphQlViaHttp(
                """
                mutation SetPublicity(${"$"}chatId: Int!, ${"$"}isInvitable: Boolean!) {
                    setPublicity(chatId: ${"$"}chatId, isInvitable: ${"$"}isInvitable) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "isInvitable" to true),
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}
