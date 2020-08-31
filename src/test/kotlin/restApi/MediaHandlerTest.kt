@file:Suppress("RedundantInnerClassModifier")

package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.testingObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun postAudioMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString()
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "audio-message", parameters)
}

private fun getAudioMessage(accessToken: String, messageId: Int): TestApplicationResponse =
    getFileMessage(accessToken, messageId, path = "audio-message")

@ExtendWith(DbExtension::class)
class MediaHandlerTest {
    @Nested
    inner class GetMediaMessage {
        @Test
        fun `A message should be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val audio = Mp3(ByteArray(1))
            val messageId = Messages.message(admin.info.id, chatId, audio)
            with(getAudioMessage(admin.accessToken, messageId)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(audio.bytes.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `An HTTP status code of 400 should be returned when retrieving a nonexistent message`() {
            val token = createVerifiedUsers(1)[0].accessToken
            assertEquals(HttpStatusCode.BadRequest, getAudioMessage(token, messageId = 1).status())
        }
    }

    @Nested
    inner class PostMediaMessage {
        @Test
        fun `An HTTP status code of 204 should be returned when a message has been created with a context`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val dummy = DummyFile("audio.mp3", bytes = 1)
            val response = postAudioMessage(admin.accessToken, dummy, chatId, contextMessageId = messageId).status()
            assertEquals(HttpStatusCode.NoContent, response)
            assertEquals(messageId, Messages.readGroupChat(chatId, userId = admin.info.id).last().node.context.id)
            assertEquals(1, AudioMessages.count())
        }

        @Test
        fun `Messaging in a nonexistent chat should fail`() {
            val token = createVerifiedUsers(1)[0].accessToken
            val dummy = DummyFile("audio.mp3", bytes = 1)
            with(postAudioMessage(token, dummy, chatId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT), body)
            }
        }

        @Test
        fun `Using an invalid message context should fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("audio.mp3", bytes = 1)
            with(postAudioMessage(admin.accessToken, dummy, chatId, contextMessageId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE), body)
            }
        }

        private fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postAudioMessage(admin.accessToken, dummy, chatId)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE), body)
            }
        }

        @Test
        fun `Uploading an invalid file type should fail`() {
            testBadRequest(DummyFile("audio.flac", bytes = 1))
        }

        @Test
        fun `Uploading an excessively large audio file should fail`() {
            testBadRequest(DummyFile("audio.mp3", Mp3.MAX_BYTES + 1))
        }

        @Test
        fun `An HTTP status code of 401 should be returned when a non-admin creates a message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = postAudioMessage(user.accessToken, DummyFile("audio.mp3", bytes = 1), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}