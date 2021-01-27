package com.neelkamath.omniChat

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.operations.READ_CHATS_QUERY
import com.neelkamath.omniChat.graphql.operations.READ_CHAT_QUERY
import com.neelkamath.omniChat.graphql.operations.REQUEST_TOKEN_SET_QUERY
import com.neelkamath.omniChat.graphql.operations.createTextMessage
import com.neelkamath.omniChat.graphql.routing.*
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class AppTest {
    @Nested
    @Suppress("ClassName")
    inner class Application_Main {
        @Test
        fun `An access token must work for queries and mutations`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val token = buildTokenSet(userId).accessToken
            assertNotEquals(
                HttpStatusCode.Unauthorized,
                executeGraphQlViaHttp(READ_CHATS_QUERY, accessToken = token).status()
            )
        }

        @Test
        fun `A token from an account with an unverified email address mustn't work for queries and mutation`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val token = buildTokenSet(userId).accessToken
            Users.update(userId, AccountUpdate(emailAddress = "new.address@example.com"))
            assertEquals(
                HttpStatusCode.Unauthorized,
                executeGraphQlViaHttp(READ_CHATS_QUERY, accessToken = token).status()
            )
        }
    }
}

/**
 * Sanity tests for encoding.
 *
 * Although encoding issues may seem to not be a problem these days,
 * https://github.com/graphql-java/graphql-java/issues/1877 shows otherwise. Had these tests not existed, such an
 * encoding problem may not have been found until technical debt from the tool at fault had already accumulated.
 */
@ExtendWith(DbExtension::class)
class EncodingTest {
    @Test
    fun `A message must allow using emoji and multiple languages`() {
        val adminId = createVerifiedUsers(1)[0].info.id
        val chatId = GroupChats.create(listOf(adminId))
        val message = MessageText("Emoji: \uD83D\uDCDA Japanese: 日 Chinese: 传/傳 Kannada: ಘ")
        createTextMessage(adminId, chatId, message)
        assertEquals(
            message,
            Messages.readGroupChat(chatId, userId = adminId)[0].node.messageId.let(TextMessages::read)
        )
    }
}

/**
 * These tests verify that the data the client receives conforms to the GraphQL spec.
 *
 * We use a GraphQL library which returns data according to the spec. However, there are two caveats which can cause the
 * data the client receives to be non-compliant with the spec:
 * 1. The GraphQL library returns `null` values for the `"data"` and `"errors"` keys. The spec mandates that these keys
 * keys either be non-`null` or not be returned at all.
 * 1. Data is serialized as JSON using the [testingObjectMapper]. The [testingObjectMapper] which may remove `null` fields if
 * incorrectly configured. The spec mandates that requested fields be returned even if they're `null`.
 */
@ExtendWith(DbExtension::class)
class SpecComplianceTest {
    @Test
    fun `The data key mustn't be returned if there was no data to be received`() {
        val login = Login(Username("u"), Password("p"))
        val keys = readGraphQlHttpResponse(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)).keys
        assertTrue("data" !in keys)
    }

    @Test
    fun `The errors key mustn't be returned if there were no errors`() {
        val login = createVerifiedUsers(1)[0].login
        val keys = readGraphQlHttpResponse(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)).keys
        assertTrue("errors" !in keys)
    }

    @Test
    fun `null fields in the data key must be returned`() {
        val admin = createVerifiedUsers(1)[0]
        val chatId = GroupChats.create(listOf(admin.info.id))
        Messages.message(admin.info.id, chatId, MessageText("t"))
        val response = readGraphQlHttpResponse(
            READ_CHAT_QUERY,
            mapOf(
                "id" to chatId,
                "privateChat_messages_last" to null,
                "privateChat_messages_before" to null,
                "groupChat_users_first" to null,
                "groupChat_users_after" to null,
                "groupChat_messages_last" to null,
                "groupChat_messages_before" to null
            ),
            admin.accessToken
        )["data"] as Map<*, *>
        val data = testingObjectMapper.convertValue<Map<String, Any?>>(response["readChat"]!!)
        val messages = testingObjectMapper.convertValue<Map<String, Any?>>(data.getValue("messages")!!)
        val edge = testingObjectMapper.convertValue<List<Map<String, Any?>>>(messages.getValue("edges")!!)[0]
        val node = testingObjectMapper.convertValue<Map<String, Any?>>(edge.getValue("node")!!)
        val context = testingObjectMapper.convertValue<Map<String, Any?>>(node.getValue("context")!!)
        assertTrue(null in context.values)
    }
}
