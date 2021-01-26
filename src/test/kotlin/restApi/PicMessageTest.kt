package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun getPicMessage(accessToken: String, messageId: Int, type: PicType): TestApplicationResponse =
    getFileMessage(accessToken, path = "pic-message", messageId, type)

private fun postPicMessage(
    accessToken: String,
    filename: String,
    fileContent: ByteArray,
    chatId: Int,
    caption: String? = null,
    contextMessageId: Int? = null,
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString(),
        "caption" to caption,
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, filename, fileContent, HttpMethod.Post, "pic-message", parameters)
}

private fun postPicMessage(
    accessToken: String,
    pic: Pic,
    chatId: Int,
    caption: String? = null,
    contextMessageId: Int? = null,
): TestApplicationResponse =
    postPicMessage(accessToken, "img.${pic.type}", pic.original, chatId, caption, contextMessageId)

@ExtendWith(DbExtension::class)
class PicMessageTest {
    @Nested
    inner class GetPicMessage {
        @Test
        fun `A pic message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = readPic("76px×57px.jpg")
            val messageId = Messages.message(admin.info.id, chatId, CaptionedPic(pic, caption = null))
            with(getPicMessage(admin.accessToken, messageId, PicType.ORIGINAL)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(pic.original.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `An HTTP status code of 400 must be returned when retrieving a nonexistent message`() {
            val token = createVerifiedUsers(1)[0].accessToken
            assertEquals(HttpStatusCode.BadRequest, getPicMessage(token, messageId = 1, PicType.ORIGINAL).status())
        }

        private fun createMessage(): Pair<String, Int> {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = CaptionedPic(readPic("1008px×756px.jpg"), caption = null)
            val messageId = Messages.message(admin.info.id, chatId, pic)
            return admin.accessToken to messageId
        }

        @Test
        fun `The original image must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getPicMessage(accessToken, messageId, PicType.ORIGINAL).byteContent
            assertTrue(PicMessages.read(messageId).pic.original.contentEquals(response))
        }

        @Test
        fun `The thumbnail must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getPicMessage(accessToken, messageId, PicType.THUMBNAIL).byteContent
            assertTrue(PicMessages.read(messageId).pic.thumbnail.contentEquals(response))
        }
    }

    @Nested
    inner class PostPicMessage {
        @Test
        fun `An HTTP status code of 204 must be returned when a captioned pic with a context has been created`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val response = postPicMessage(
                admin.accessToken,
                readPic("76px×57px.jpg"),
                chatId,
                "caption",
                contextMessageId = messageId,
            )
            assertEquals(HttpStatusCode.NoContent, response.status())
            assertEquals(messageId, Messages.readGroupChat(chatId, userId = admin.info.id).last().node.context.id)
            assertEquals(1, PicMessages.count())
        }

        @Test
        fun `Messaging in a nonexistent chat must fail`() {
            val token = createVerifiedUsers(1)[0].accessToken
            with(postPicMessage(token, readPic("76px×57px.jpg"), chatId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT), body)
            }
        }

        @Test
        fun `Using an invalid message context must fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postPicMessage(admin.accessToken, readPic("76px×57px.jpg"), chatId, contextMessageId = 1)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE), body)
            }
        }

        private fun testBadRequest(filename: String, fileContent: ByteArray) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postPicMessage(admin.accessToken, filename, fileContent, chatId)) {
                assertEquals(HttpStatusCode.BadRequest, status())
                val body = testingObjectMapper.readValue<InvalidMediaMessage>(content!!)
                assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE), body)
            }
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit =
            "76px×57px.webp".let { testBadRequest(it, readBytes(it)) }

        @Test
        fun `Uploading an excessively large file must fail`(): Unit =
            "5.6MB.jpg".let { testBadRequest(it, readBytes(it)) }

        @Test
        fun `Using an invalid caption must fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            postPicMessage(admin.accessToken, readPic("76px×57px.jpg"), chatId, caption = "")
        }

        @Test
        fun `An HTTP status code of 401 must be returned when a non-admin creates a message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = postPicMessage(user.accessToken, readPic("76px×57px.jpg"), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}
