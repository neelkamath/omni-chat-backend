package com.neelkamath.omniChatBackend.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.db.tables.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun getPicMessage(accessToken: String? = null, messageId: Int, type: PicType): TestApplicationResponse =
    getFileMessage(accessToken, path = "pic-message", messageId, type)

private fun postPicMessage(
    accessToken: String,
    filename: String,
    fileContent: ByteArray,
    chatId: Int,
    contextMessageId: Int? = null,
    caption: String? = null,
): TestApplicationResponse = uploadMultipart(
    accessToken,
    HttpMethod.Post,
    "pic-message",
    listOfNotNull(
        buildFileItem(filename, fileContent),
        buildFormItem("chat-id", chatId.toString()),
        buildFormItem("context-message-id", contextMessageId.toString()).takeIf { contextMessageId != null },
        buildFormItem("caption", caption.toString()).takeIf { caption != null },
    ),
)

private fun postPicMessage(
    accessToken: String,
    pic: Pic,
    chatId: Int,
    contextMessageId: Int? = null,
    caption: String? = null,
): TestApplicationResponse =
    postPicMessage(accessToken, "pic.${pic.type}", pic.original, chatId, contextMessageId, caption)

@ExtendWith(DbExtension::class)
class PicMessageTest {
    @Nested
    inner class GetPicMessage {
        @Test
        fun `A pic message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = readPic("76px×57px.jpg")
            val messageId = Messages.message(admin.info.id, chatId, CaptionedPic(pic, caption = null))
            val response = getPicMessage(admin.accessToken, messageId, PicType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(pic.original.contentEquals(response.byteContent!!))
        }

        @Test
        fun `An HTTP status code of 401 must be returned when retrieving a nonexistent message`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.Unauthorized, getPicMessage(token, messageId = 1, PicType.ORIGINAL).status())
        }

        private fun createMessage(): Pair<String, Int> {
            val admin = createVerifiedUsers(1).first()
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
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val response = postPicMessage(
                admin.accessToken,
                readPic("76px×57px.jpg"),
                chatId,
                contextMessageId = messageId,
                "caption",
            )
            assertEquals(HttpStatusCode.NoContent, response.status())
            assertEquals(messageId, Messages.readGroupChat(chatId, userId = admin.info.id).last().node.context.id)
            assertEquals(1, PicMessages.count())
        }

        @Test
        fun `Messaging in a nonexistent chat must fail`() {
            val token = createVerifiedUsers(1).first().accessToken
            val response = postPicMessage(token, readPic("76px×57px.jpg"), chatId = 1)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!)
            assertEquals(InvalidPicMessage(InvalidPicMessage.Reason.USER_NOT_IN_CHAT), body)
        }

        @Test
        fun `Using an invalid message context must fail`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val response = postPicMessage(admin.accessToken, readPic("76px×57px.jpg"), chatId, contextMessageId = 1)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!)
            assertEquals(InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CONTEXT_MESSAGE), body)
        }

        private fun testBadRequest(filename: String) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val response = postPicMessage(admin.accessToken, filename, readBytes(filename), chatId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!)
            assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE), body)
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("5.6MB.jpg")

        @Test
        fun `Using an invalid caption must fail`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val response = postPicMessage(admin.accessToken, readPic("76px×57px.jpg"), chatId, caption = "")
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!)
            assertEquals(InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CAPTION), body)
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
