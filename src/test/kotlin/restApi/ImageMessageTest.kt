package com.neelkamath.omniChatBackend.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

private fun getImageMessage(messageId: Int, type: ImageType, accessToken: String? = null): TestApplicationResponse =
    getFileMessage(path = "image-message", messageId, type, accessToken)

private fun postImageMessage(
    accessToken: String,
    chatId: Int,
    filename: String,
    caption: String? = null,
    contextMessageId: Int? = null,
): TestApplicationResponse = uploadMultipart(
    HttpMethod.Post,
    "image-message",
    listOfNotNull(
        buildFileItem(filename, readBytes(filename)),
        buildFormItem("chat-id", chatId.toString()),
        buildFormItem("context-message-id", contextMessageId.toString()).takeIf { contextMessageId != null },
        buildFormItem("caption", caption.toString()).takeIf { caption != null },
    ),
    accessToken = accessToken,
)

@ExtendWith(DbExtension::class)
class ImageMessageTest {
    @Nested
    inner class GetImageMessage {
        @Test
        fun `An image message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val image = readImage("76px×57px.jpg")
            val messageId = Messages.message(admin.userId, chatId, CaptionedImage(image, caption = null))
            val response = getImageMessage(messageId, ImageType.ORIGINAL, admin.accessToken)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(image.original, response.byteContent!!)
        }

        @Test
        fun `An HTTP status code of 401 must be returned when retrieving a non-existing message`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(
                HttpStatusCode.Unauthorized,
                getImageMessage(messageId = 1, ImageType.ORIGINAL).status(),
                token
            )
        }

        private fun createMessage(): Pair<String, Int> {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val image = CaptionedImage(readImage("1008px×756px.jpg"), caption = null)
            val messageId = Messages.message(admin.userId, chatId, image)
            return admin.accessToken to messageId
        }

        @Test
        fun `The original image must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getImageMessage(messageId, ImageType.ORIGINAL, accessToken).byteContent
            assertContentEquals(ImageMessages.readImage(messageId, ImageType.ORIGINAL).bytes, response)
        }

        @Test
        fun `The thumbnail must be sent when requested`() {
            val (accessToken, messageId) = createMessage()
            val response = getImageMessage(messageId, ImageType.THUMBNAIL, accessToken).byteContent
            assertContentEquals(ImageMessages.readImage(messageId, ImageType.THUMBNAIL).bytes, response)
        }
    }

    @Nested
    inner class PostImageMessage {
        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId), listOf(user.userId), isBroadcast = true)
            val userStatus = postImageMessage(user.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.BadRequest, userStatus)
            assertEquals(0, Messages.count())
            val adminStatus = postImageMessage(admin.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.NoContent, adminStatus)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `The message must get created sans context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val actual = postImageMessage(admin.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val contextMessageId = Messages.message(admin.userId, chatId)
            val actual =
                postImageMessage(admin.accessToken, chatId, "76px×57px.jpg", contextMessageId = contextMessageId)
                    .status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId))
            val response = postImageMessage(user.accessToken, chatId, "76px×57px.jpg")
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidImageMessage>(response.content!!).reason
            assertEquals(InvalidImageMessage.Reason.USER_NOT_IN_CHAT, reason)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val admin = createVerifiedUsers(1).first()
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(admin.userId)) }
            val contextMessageId = Messages.message(admin.userId, chat1Id)
            val response = postImageMessage(
                admin.accessToken,
                chat2Id, "76px×57px.jpg",
                contextMessageId = contextMessageId,
            )
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidImageMessage>(response.content!!).reason
            assertEquals(InvalidImageMessage.Reason.INVALID_CONTEXT_MESSAGE, reason)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1, user2) = createVerifiedUsers(2)
            val chatId = PrivateChats.create(user1.userId, user2.userId)
            val contextMessageId = Messages.message(user1.userId, chatId)
            PrivateChatDeletions.create(chatId, user1.userId)
            val response =
                postImageMessage(user1.accessToken, chatId, "76px×57px.jpg", contextMessageId = contextMessageId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val reason = testingObjectMapper.readValue<InvalidImageMessage>(response.content!!).reason
            assertEquals(InvalidImageMessage.Reason.INVALID_CONTEXT_MESSAGE, reason)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `The message must get created with a caption`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val filename = "76px×57px.jpg"
            val caption = MessageText("c")
            val actual = postImageMessage(admin.accessToken, chatId, filename, caption.value).status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).last()
            val expected = CaptionedImage(readImage(filename), caption)
            assertEquals(expected, ImageMessages.readCaptionedImage(messageId))
        }

        private fun assertInvalidImageMessage(
            reason: InvalidImageMessage.Reason,
            filename: String,
            caption: String? = null,
        ) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val response = postImageMessage(admin.accessToken, chatId, filename, caption)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val actual = testingObjectMapper.readValue<InvalidImageMessage>(response.content!!).reason
            assertEquals(reason, actual)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Attempting to create a message with an invalid file type must fail`(): Unit =
            assertInvalidImageMessage(InvalidImageMessage.Reason.INVALID_FILE, "76px×57px.webp")

        @Test
        fun `Attempting to create a message with an invalid file size must fail`(): Unit =
            assertInvalidImageMessage(InvalidImageMessage.Reason.INVALID_FILE, "3.9MB.jpg")

        @Test
        fun `Attempting to create a message with an invalid caption must fail`(): Unit =
            assertInvalidImageMessage(InvalidImageMessage.Reason.INVALID_CAPTION, "76px×57px.jpg", caption = "")
    }
}
