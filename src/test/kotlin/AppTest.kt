package com.neelkamath.omniChat

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.db.chats.GroupChats
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.callGraphQlQueryOrMutation
import graphql.operations.mutations.createAccount
import graphql.operations.mutations.createGroupChat
import graphql.operations.mutations.createMessage
import graphql.operations.queries.READ_ACCOUNT_QUERY
import graphql.operations.queries.REQUEST_TOKEN_SET_QUERY
import graphql.operations.queries.requestTokenSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotHaveKey
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun checkHealth(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

class GetHealthCheckTest : FunSpec({
    test("A health check should respond with an HTTP status code of 204") {
        checkHealth().status() shouldBe HttpStatusCode.NoContent
    }
})

/**
 * Sanity tests for encoding.
 *
 * Don't write a test for every GraphQL operation saving text; a few are enough. Although encoding issues may seem to
 * not be a problem these days, https://github.com/graphql-java/graphql-java/issues/1877 shows otherwise. Had these
 * tests not existed, such an encoding problem may not have been found until technical debt from the tool at fault
 * had already accumulated.
 */
class EncodingTest : FunSpec({
    test("A group chat's title should allow emoji") {
        val token = createSignedInUsers(1)[0].accessToken
        val title = "Title \uD83D\uDCDA"
        val chatId = createGroupChat(token, NewGroupChat(title))
        GroupChats.readChat(chatId).title shouldBe title
    }

    fun testMessage(message: String) {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        createMessage(token, chatId, message)
        Messages.readGroupChat(chatId)[0].node.text shouldBe message
    }

    test("A message should allow emoji") { testMessage("message \uD83D\uDCDA") }

    test("A message should allow using multiple languages") { testMessage("Japanese: 日 Chinese: 传/傳 Kannada: ಘ") }
})

/**
 * These tests verify that the data the client receives conforms to the GraphQL spec.
 *
 * We use a GraphQL library which returns data according to the spec. However, there are two caveats which can cause the
 * data the client receives to be non-compliant with the spec:
 * 1. The GraphQL library returns `null` values for the `"data"` and `"errors"` keys. The spec mandates that these keys
 * keys either be non-`null` or not be returned at all.
 * 1. Data is serialized as JSON using the [objectMapper]. The [objectMapper] which may remove `null` fields if
 * incorrectly configured. The spec mandates that requested fields be returned even if they're `null`.
 */
class SpecComplianceTest : FunSpec({
    test("""The "data" key shouldn't be returned if there was no data to be received""") {
        val variables = mapOf("login" to Login("username", "password"))
        callGraphQlQueryOrMutation(REQUEST_TOKEN_SET_QUERY, variables) shouldNotHaveKey "data"
    }

    test("""The "errors" key shouldn't be returned if there were no errors""") {
        val login = createSignedInUsers(1)[0].login
        callGraphQlQueryOrMutation(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)) shouldNotHaveKey
                "errors"
    }

    test("""null fields in the "data" key should be returned""") {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        verifyEmailAddress(account.username)
        val login = Login(account.username, account.password)
        val accessToken = requestTokenSet(login).accessToken
        val response = callGraphQlQueryOrMutation(READ_ACCOUNT_QUERY, accessToken = accessToken)["data"] as Map<*, *>
        objectMapper.convertValue<Map<String, Any>>(response["readAccount"]!!) shouldContain Pair("firstName", null)
    }
})