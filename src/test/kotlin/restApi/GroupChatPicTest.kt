package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun getGroupChatPic(accessToken: String? = null, chatId: Int, type: PicType): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("chat-id" to chatId.toString(), "pic-type" to type.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "group-chat-pic?$parameters") {
            if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        }.response
    }

private fun patchGroupChatPic(
    accessToken: String,
    filename: String,
    fileContent: ByteArray,
    chatId: Int,
): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(accessToken, filename, fileContent, HttpMethod.Patch, "group-chat-pic", parameters)
}

private fun patchGroupChatPic(accessToken: String, pic: Pic, chatId: Int): TestApplicationResponse =
    patchGroupChatPic(accessToken, "pic.${pic.type}", pic.original, chatId)

@ExtendWith(DbExtension::class)
class GroupChatPicTest {
    @Nested
    inner class PatchGroupChatPic {
        @Test
        fun `Updating the pic must cause an HTTP status code of 204 to be received, and the DB to be updated`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = readPic("76px×57px.jpg")
            assertEquals(HttpStatusCode.NoContent, patchGroupChatPic(admin.accessToken, pic, chatId).status())
            assertEquals(pic, GroupChats.readPic(chatId))
        }

        private fun testBadRequest(filename: String) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val response = patchGroupChatPic(admin.accessToken, filename, readBytes(filename), chatId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("5.6MB.jpg")

        @Test
        fun `An HTTP status code of 401 must be received when a non-admin updates the pic`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = patchGroupChatPic(user.accessToken, readPic("76px×57px.jpg"), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
            assertNull(GroupChats.readPic(chatId))
        }
    }

    private data class ChatPic(val chatId: Int, val pic: Pic)

    @Nested
    inner class GetGroupChatPic {
        @Test
        fun `A pic must be retrieved with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = readPic("76px×57px.jpg")
            GroupChats.updatePic(chatId, pic)
            val response = getGroupChatPic(admin.accessToken, chatId, PicType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(pic.original.contentEquals(response.byteContent))
        }

        @Test
        fun `An HTTP status code of 204 must be received when reading a nonexistent pic`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val status = getGroupChatPic(admin.accessToken, chatId, PicType.ORIGINAL).status()
            assertEquals(HttpStatusCode.NoContent, status)
        }

        @Test
        fun `An HTTP status code of 400 must be received if the chat doesn't exist`(): Unit =
            assertEquals(HttpStatusCode.BadRequest, getGroupChatPic(chatId = 1, type = PicType.ORIGINAL).status())

        private fun createGroupChat(): ChatPic {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val pic = readPic("1008px×756px.jpg")
            GroupChats.updatePic(chatId, pic)
            return ChatPic(chatId, pic)
        }

        @Test
        fun `The original pic must be received when requested`() {
            val (chatId, pic) = createGroupChat()
            val response = getGroupChatPic(chatId = chatId, type = PicType.ORIGINAL).byteContent
            assertTrue(pic.original.contentEquals(response))
        }

        @Test
        fun `The thumbnail must be received when requested`() {
            val (chatId, pic) = createGroupChat()
            val response = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).byteContent
            assertTrue(pic.thumbnail.contentEquals(response))
        }

        @Test
        fun `The pic must be retrieved from a public chat sans access token`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val status = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.NoContent, status)
        }

        @Test
        fun `The pic must not be retrieved from a chat sans access token`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val status = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        @Test
        fun `The pic must not be retrieved from a chat the user isn't in`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id))
            val status = getGroupChatPic(user.accessToken, chatId, PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}
