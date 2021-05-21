package com.neelkamath.omniChatBackend.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

private fun getPicMessage(accessToken: String? = null, messageId: Int, type: PicType): TestApplicationResponse =
    getFileMessage(accessToken, path = "pic-message", messageId, type)

private fun postPicMessage(
    accessToken: String,
    chatId: Int,
    filename: String,
    caption: String? = null,
    contextMessageId: Int? = null,
): TestApplicationResponse = uploadMultipart(
    accessToken,
    HttpMethod.Post,
    "pic-message",
    listOfNotNull(
        buildFileItem(filename, readBytes(filename)),
        buildFormItem("chat-id", chatId.toString()),
        buildFormItem("context-message-id", contextMessageId.toString()).takeIf { contextMessageId != null },
        buildFormItem("caption", caption.toString()).takeIf { caption != null },
    ),
)

@ExtendWith(DbExtension::class)
class PicMessageTest {
    @Nested
    inner class GetPicMessage {
        @Test
        fun `A pic message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val pic = readPic("76px×57px.jpg")
            val messageId = Messages.message(admin.userId, chatId, CaptionedPic(pic, caption = null))
            val response = getPicMessage(admin.accessToken, messageId, PicType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(pic.original.contentEquals(response.byteContent!!))
        }

        @Test
        fun `An HTTP status code of 401 must be returned when retrieving a non-existing message`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.Unauthorized, getPicMessage(token, messageId = 1, PicType.ORIGINAL).status())
        }

        private fun createMessage(): Pair<String, Int> {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val pic = CaptionedPic(readPic("1008px×756px.jpg"), caption = null)
            val messageId = Messages.message(admin.userId, chatId, pic)
            return admin.accessToken to messageId
        }

        @Test
        fun `The original image must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getPicMessage(accessToken, messageId, PicType.ORIGINAL).byteContent
            assertTrue(PicMessages.readPic(messageId, PicType.ORIGINAL).contentEquals(response))
        }

        @Test
        fun `The thumbnail must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getPicMessage(accessToken, messageId, PicType.THUMBNAIL).byteContent
            assertTrue(PicMessages.readPic(messageId, PicType.THUMBNAIL).contentEquals(response))
        }
    }

    @Nested
    inner class PostPicMessage {
        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.userId), listOf(user.userId), isBroadcast = true)
            mapOf(admin to HttpStatusCode.NoContent, user to HttpStatusCode.Unauthorized).forEach {
                val actual = postPicMessage(it.key.accessToken, chatId, "76px×57px.jpg").status()
                assertEquals(it.value, actual)
                assertEquals(1, Messages.count())
            }
        }

        @Test
        fun `The message must get created sans context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val actual = postPicMessage(admin.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val contextMessageId = Messages.message(admin.userId, chatId)
            val actual = postPicMessage(admin.accessToken, chatId, "76px×57px.jpg", contextMessageId = contextMessageId)
                .status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.userId))
            val response = postPicMessage(user.accessToken, chatId, "76px×57px.jpg")
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!).reason
            assertEquals(InvalidPicMessage.Reason.USER_NOT_IN_CHAT, reason)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val admin = createVerifiedUsers(1).first()
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(listOf(admin.userId)) }
            val contextMessageId = Messages.message(admin.userId, chat1Id)
            val response = postPicMessage(
                admin.accessToken,
                chat2Id, "76px×57px.jpg",
                contextMessageId = contextMessageId,
            )
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!).reason
            assertEquals(InvalidPicMessage.Reason.INVALID_CONTEXT_MESSAGE, reason)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1, user2) = createVerifiedUsers(2)
            val chatId = PrivateChats.create(user1.userId, user2.userId)
            val contextMessageId = Messages.message(user1.userId, chatId)
            PrivateChatDeletions.create(chatId, user1.userId)
            val response =
                postPicMessage(user1.accessToken, chatId, "76px×57px.jpg", contextMessageId = contextMessageId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!).reason
            assertEquals(InvalidPicMessage.Reason.INVALID_CONTEXT_MESSAGE, reason)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `The message must get created with a caption`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val filename = "76px×57px.jpg"
            val caption = MessageText("c")
            val actual = postPicMessage(admin.accessToken, chatId, filename, caption.value).status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).last()
            val expected = CaptionedPic(readPic(filename), caption)
            assertEquals(expected, PicMessages.readCaptionedPic(messageId))
        }

        private fun assertInvalidPicMessage(
            reason: InvalidPicMessage.Reason,
            filename: String,
            caption: String? = null,
        ) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.userId))
            val response = postPicMessage(admin.accessToken, chatId, filename, caption)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val actual = testingObjectMapper.readValue<InvalidPicMessage>(response.content!!).reason
            assertEquals(reason, actual)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Attempting to create a message with an invalid file type must fail`(): Unit =
            assertInvalidPicMessage(InvalidPicMessage.Reason.INVALID_FILE, "76px×57px.webp")

        @Test
        fun `Attempting to create a message with an invalid file size must fail`(): Unit =
            assertInvalidPicMessage(InvalidPicMessage.Reason.INVALID_FILE, "5.6MB.jpg")

        @Test
        fun `Attempting to create a message with an invalid caption must fail`(): Unit =
            assertInvalidPicMessage(InvalidPicMessage.Reason.INVALID_CAPTION, "76px×57px.jpg", caption = "")
    }
}
