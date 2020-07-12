package com.neelkamath.omniChat

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.graphql.operations.READ_ACCOUNT_QUERY
import com.neelkamath.omniChat.graphql.operations.REQUEST_TOKEN_SET_QUERY
import com.neelkamath.omniChat.graphql.operations.createMessage
import com.neelkamath.omniChat.routing.readGraphQlHttpResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotHaveKey
import io.kotest.matchers.shouldBe

/**
 * Sanity tests for encoding.
 *
 * Although encoding issues may seem to not be a problem these days,
 * https://github.com/graphql-java/graphql-java/issues/1877 shows otherwise. Had these tests not existed, such an
 * encoding problem may not have been found until technical debt from the tool at fault had already accumulated.
 */
class EncodingTest : FunSpec({
    test("A message should allow using emoji and multiple languages") {
        val adminId = createVerifiedUsers(1)[0].info.id
        val chatId = GroupChats.create(adminId, buildNewGroupChat())
        val message = TextMessage("\uD83D\uDCDA Japanese: 日 Chinese: 传/傳 Kannada: ಘ")
        createMessage(adminId, chatId, message)
        Messages.readGroupChat(chatId)[0].node.text shouldBe message
    }
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
        val login = Login(Username("username"), Password("password"))
        readGraphQlHttpResponse(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)) shouldNotHaveKey "data"
    }

    test("""The "errors" key shouldn't be returned if there were no errors""") {
        val login = createVerifiedUsers(1)[0].login
        readGraphQlHttpResponse(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)) shouldNotHaveKey
                "errors"
    }

    test("""null fields in the "data" key should be returned""") {
        val account = NewAccount(Username("username"), Password("password"), "username@example.com")
        createUser(account)
        val userId = readUserByUsername(account.username).id
        val accessToken = buildAuthToken(userId).accessToken
        val response = readGraphQlHttpResponse(READ_ACCOUNT_QUERY, accessToken = accessToken)["data"] as Map<*, *>
        objectMapper.convertValue<Map<String, Any>>(response["readAccount"]!!) shouldContain Pair("firstName", null)
    }
})