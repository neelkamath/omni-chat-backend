package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun getGroupChatImage(chatId: Int, type: ImageType): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("chat-id" to chatId.toString(), "image-type" to type.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "group-chat-image?$parameters").response
    }

private fun patchGroupChatImage(accessToken: String, chatId: Int, filename: String): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(filename, readBytes(filename), HttpMethod.Patch, "group-chat-image", parameters, accessToken)
}

@ExtendWith(DbExtension::class)
class GroupChatImageTest {
    @Nested
    inner class PatchGroupChatImage {
        @Test
        fun `Updating the image must cause an HTTP status code of 204 to be received, and the DB to be updated`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val filename = "76px×57px.jpg"
            val image = readImage(filename)
            assertEquals(HttpStatusCode.NoContent, patchGroupChatImage(admin.accessToken, chatId, filename).status())
            val original = GroupChats.readImage(chatId, ImageType.ORIGINAL)!!.bytes
            assertContentEquals(image.original, original)
            val thumbnail = GroupChats.readImage(chatId, ImageType.THUMBNAIL)!!.bytes
            assertContentEquals(image.thumbnail, thumbnail)
        }

        private fun testBadRequest(filename: String) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val actual = patchGroupChatImage(admin.accessToken, chatId, filename).status()
            assertEquals(HttpStatusCode.BadRequest, actual)
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("3.9MB.jpg")

        @Test
        fun `An HTTP status code of 401 must be received when a non-admin updates the image`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId), listOf(user.userId))
            val actual = patchGroupChatImage(user.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.Unauthorized, actual)
            assertNull(GroupChats.readImage(chatId, ImageType.ORIGINAL))
        }
    }

    private data class ChatImage(val chatId: Int, val image: ProcessedImage)

    @Nested
    inner class GetGroupChatImage {
        @Test
        fun `An image must be retrieved with an HTTP status code of 200`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val image = readImage("76px×57px.jpg")
            GroupChats.updateImage(chatId, image)
            val response = getGroupChatImage(chatId, ImageType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(image.original, response.byteContent)
        }

        @Test
        fun `An HTTP status code of 204 must be received when reading a non-existing image`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val status = getGroupChatImage(chatId, ImageType.ORIGINAL).status()
            assertEquals(HttpStatusCode.NoContent, status)
        }

        @Test
        fun `An HTTP status code of 400 must be received if the chat doesn't exist`(): Unit =
            assertEquals(HttpStatusCode.BadRequest, getGroupChatImage(chatId = 1, type = ImageType.ORIGINAL).status())

        private fun createGroupChat(): ChatImage {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val image = readImage("1008px×756px.jpg")
            GroupChats.updateImage(chatId, image)
            return ChatImage(chatId, image)
        }

        @Test
        fun `The original image must be received when requested`() {
            val (chatId, image) = createGroupChat()
            val response = getGroupChatImage(chatId = chatId, type = ImageType.ORIGINAL).byteContent
            assertContentEquals(image.original, response)
        }

        @Test
        fun `The thumbnail must be received when requested`() {
            val (chatId, image) = createGroupChat()
            val response = getGroupChatImage(chatId = chatId, type = ImageType.THUMBNAIL).byteContent
            assertContentEquals(image.thumbnail, response)
        }
    }
}
