package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
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

private fun getPicMessage(accessToken: String, messageId: Int): TestApplicationResponse =
    getFileMessage(accessToken, messageId, path = "pic-message")

private fun postPicMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    caption: String? = null,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString(),
        "caption" to caption
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "pic-message", parameters)
}

@ExtendWith(DbExtension::class)
class PicMessageTest {
    @Nested
    inner class GetPicMessage {
        @Test
        fun `A pic message should be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val messageId = Messages.message(admin.info.id, chatId, CaptionedPic(pic, caption = null))
            with(getPicMessage(admin.accessToken, messageId)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(pic.bytes.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `An HTTP status code of 400 should be returned when retrieving a nonexistent message`() {
            val token = createVerifiedUsers(1)[0].accessToken
            assertEquals(HttpStatusCode.BadRequest, getPicMessage(token, messageId = 1).status())
        }
    }

    @Nested
    inner class PostPicMessage {
        @Test
        fun `An HTTP status code of 204 should be returned when a captioned pic with a context has been created`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val dummy = DummyFile("pic.png", bytes = 1)
            assertEquals(
                HttpStatusCode.NoContent,
                postPicMessage(admin.accessToken, dummy, chatId, "caption", contextMessageId = messageId).status()
            )
            assertEquals(messageId, Messages.readGroupChat(chatId, userId = admin.info.id).last().node.context.id)
            assertEquals(1, PicMessages.count())
        }

        @Test
        fun `Messaging in a nonexistent chat should fail`() {
            val token = createVerifiedUsers(1)[0].accessToken
            val dummy = DummyFile("pic.png", bytes = 1)
            with(postPicMessage(token, dummy, chatId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT), body)
            }
        }

        @Test
        fun `Using an invalid message context should fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            with(postPicMessage(admin.accessToken, dummy, chatId, contextMessageId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE), body)
            }
        }

        private fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postPicMessage(admin.accessToken, dummy, chatId)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE), body)
            }
        }

        @Test
        fun `Uploading an invalid file type should fail`() {
            testBadRequest(DummyFile("pic.webp", bytes = 1))
        }

        @Test
        fun `Uploading an excessively large audio file should fail`() {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }

        @Test
        fun `Using an invalid caption should fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            postPicMessage(admin.accessToken, dummy, chatId, caption = "")
        }

        @Test
        fun `An HTTP status code of 401 should be returned when a non-admin creates a message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = postPicMessage(user.accessToken, DummyFile("pic.png", bytes = 1), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}
